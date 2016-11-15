package com.atos.indigo.reposync;

import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.atos.indigo.reposync.common.RepositoryServiceProviderServiceServer;
import com.atos.indigo.reposync.providers.OpenNebulaRepositoryServiceProvider;
import com.atos.indigo.reposync.providers.OpenStackRepositoryServiceProvider;
import com.atos.indigo.reposync.providers.RepositoryServiceProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;

import org.glassfish.jersey.server.ChunkedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.NotAuthorizedException;

/**
 * Root resource (exposed at "v1.0" path)
 */
public class RepositoryServiceProviderServiceImpl
    implements RepositoryServiceProviderServiceServer {

  private static final Logger logger = LoggerFactory.getLogger(RepositoryServiceProvider.class);
  public static final String IMG_SEPARATOR = "&&";

  RepositoryServiceProvider provider = null;

  DockerClient dockerClient = null;

  ObjectMapper mapper = new ObjectMapper();

  /**
   * Configure the default backend reading the system configuration.
   * @throws ConfigurationException If the configuration is not defined or is not correct.
   */
  public RepositoryServiceProviderServiceImpl() throws ConfigurationException {
    this.provider = (ReposyncTags.REPOSYNC_BACKEND_OS.toLowerCase().equals(
      ConfigurationManager.getProperty(ReposyncTags.REPOSYNC_BACKEND).toLowerCase()))
      ? new OpenStackRepositoryServiceProvider()
      : new OpenNebulaRepositoryServiceProvider();



    DockerClientConfig.DockerClientConfigBuilder configBuilder =
        new DockerClientConfig.DockerClientConfigBuilder();

    Properties dockerProps = ConfigurationManager.getDockerProperties();
    if (dockerProps != null && !dockerProps.isEmpty()) {
      configBuilder.withProperties(dockerProps);
    }

    DockerClientConfig config = configBuilder.build();
    this.dockerClient = DockerClientBuilder.getInstance(config).build();
  }

  /**
   * For testing purposes only.
   * @param provider Mock provider
   * @param client Mock docker client
   */
  public RepositoryServiceProviderServiceImpl(RepositoryServiceProvider provider,
                                              DockerClient client) {
    this.provider = provider;
    this.dockerClient = client;
  }

  /**
   * Get a list of images present in the IaaS platform filtering by name.
   * @param filter Optional filter by name. It should be a regular expression.
   * @return The list of images found.
   */
  @Authorized
  public List<ImageInfoBean> images(String filter) {
    logger.debug("Retrieving image list");
    return provider.images(filter);
  }


  /**
   * Force the download and register of a docker image.
   * @param imageName Image name in Docker Hub.
   * @param tag Desired tag. This parameter is optiona. If not present, latest will be used.
   * @return Return asynchronously the information of the new image.
   */
  @Authorized
  public ChunkedOutput<ImageInfoBean> pull( final String imageName, final String tag) {

    final ChunkedOutput<ImageInfoBean> output = new ChunkedOutput<>(ImageInfoBean.class);

    final PullImageCmd pullCmd = dockerClient.pullImageCmd(imageName);

    if (tag != null) {
      pullCmd.withTag(tag);
    }

    final String finalTag = (tag != null) ? tag : "latest";

    logger.debug("Pulling image " + imageName + "with tag " + finalTag);

    new Thread() {
      @Override
      public void run() {
        try {
          pullCmd.exec(new PullImageResultCallback()).awaitSuccess();

          InspectImageResponse img = dockerClient
              .inspectImageCmd(imageName + ":" + finalTag).exec();

          if (img != null) {
            output.write(provider.imageUpdated(imageName, finalTag, img, dockerClient));
          }
        } catch (Exception e) {
          logger.error("Error pulling image " + imageName + ":" + finalTag, e);
        } finally {
          try {
            output.close();
          } catch (IOException e) {
            logger.error("Error writing output response while pull operation", e);
          }
        }

      }
    }.start();

    return output;
  }

  /**
   * Delete an image provided its id.
   * @param imageId Image Id to delete.
   * @return Success status.
   */
  @Authorized
  public ActionResponseBean delete(String imageId) {

    logger.debug("Deleting image with id " + imageId);

    return provider.delete(imageId);
  }

  /**
   * Webhook that will be called when a new image is pushed or updated to the Indigo repository.
   * @param token The secret token needed to verify the origin of the call.
   * @param payload The webhook payload sent by DockerHub.
   */
  public ChunkedOutput<ImageInfoBean> notify(String token, ObjectNode payload) {

    logger.debug("Received webhook notification. Payload: \n" + payload.toString());


    if (token != null && token.equals(
        ConfigurationManager.getProperty(ReposyncTags.REPOSYNC_TOKEN))) {
      JsonNode pushData = payload.get("push_data");
      String tag = pushData.get("tag").asText();

      JsonNode repoInfo = payload.get("repository");
      String repoName = repoInfo.get("repo_name").asText();

      return pull(repoName, tag);
    } else {
      throw new NotAuthorizedException("Authorization token needed");
    }
  }

  /**
   * Force the synchronization of the configured repositories.
   * It will pull the images and then execute an update on each of them
   * @return Asynchronously returns each updated image.
   */
  @Authorized
  public ChunkedOutput<String> sync() {

    logger.debug("Syncing repository");

    final ChunkedOutput<String> output = new ChunkedOutput<>(String.class);

    List<String> repoList = ConfigurationManager.getRepoList();
    if (!repoList.isEmpty()) {
      final List<Thread> threads = new ArrayList<>();

      for (String repo : repoList) {
        String[] repoInfo = repo.split(":");
        String repoName = repoInfo[0];
        if (repo != null && !repo.trim().isEmpty()) {
          final String finalRepo = repoName.trim();
          final String finalTag = (repoInfo.length > 1) ? repoInfo[1] : "latest";
          threads.add(
              new Thread() {
                @Override
                public void run() {
                  synchronized (dockerClient) {

                    String fullImgName = finalRepo + ":" + finalTag;

                    logger.debug("Syncing image " + fullImgName);

                    dockerClient.pullImageCmd(fullImgName).exec(new PullImageResultCallback())
                      .awaitSuccess();

                    InspectImageResponse img = dockerClient.inspectImageCmd(fullImgName)
                        .exec();

                    try {
                      output.write(mapper.writeValueAsString(
                          provider.imageUpdated(finalRepo, finalTag, img, dockerClient))
                          + IMG_SEPARATOR);
                    } catch (IOException e) {
                      logger.error(
                          "Error updating image " + fullImgName + " during synchronization", e);
                    }

                    threads.remove(this);
                    if (threads.isEmpty()) {
                      try {
                        output.close();
                      } catch (IOException e) {
                        logger.error("Error closing output", e);
                      }
                    }
                  }
                }
              }
          );
        } else {
          logger.error("Invalid image name found in repolist file: " + repo);
          writeEmptySync(output);
        }
      }

      threads.forEach(thread -> thread.start());
    } else {
      writeEmptySync(output);
    }



    return output;
  }

  private void writeEmptySync(ChunkedOutput<String> output) {
    try {
      output.write("{}" + IMG_SEPARATOR);
      output.close();
    } catch (IOException e) {
      logger.error("Error writing empty sync response",e);
    }
  }

  public void setProvider(RepositoryServiceProvider provider) {
    this.provider = provider;
  }

  public void setDockerClient(DockerClient dockerClient) {
    this.dockerClient = dockerClient;
  }
}
