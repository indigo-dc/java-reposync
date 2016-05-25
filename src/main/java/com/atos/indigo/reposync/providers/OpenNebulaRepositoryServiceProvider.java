package com.atos.indigo.reposync.providers;

import com.atos.indigo.reposync.ConfigurationManager;
import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.Image;

import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;
import org.opennebula.client.OneResponse;
import org.opennebula.client.datastore.Datastore;
import org.opennebula.client.datastore.DatastorePool;
import org.opennebula.client.image.ImagePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.PathParam;

/**
 * Created by jose on 20/04/16.
 */
public class OpenNebulaRepositoryServiceProvider implements RepositoryServiceProvider {

  public static final String ONEDOCK_DATASTORE_NAME = "onedock";
  public static final String DOCKER_PREFIX = "docker://";
  public static final String DOCKER_ID = "DOCKER_ID";
  public static final String DOCKER_NAME = "DOCKER_NAME";
  public static final String DOCKER_TAG = "DOCKER_TAG";
  private static final Logger logger = LoggerFactory.getLogger(
          OpenNebulaRepositoryServiceProvider.class);
  private static final String ONE_XMLRPC = ConfigurationManager.getProperty("ONE_XMLRPC");
  private static final String ONE_AUTH = ConfigurationManager.getProperty("ONE_AUTH");

  private static String readCredentials(String oneAuth) {
    if (oneAuth != null) {
      File authFile = new File(oneAuth);
      if (authFile.exists()) {
        try {
          BufferedReader reader = new BufferedReader(new FileReader(authFile));
          String auth = reader.readLine();
          if (auth != null && !auth.isEmpty()) {
            return auth.trim();
          } else {
            logger.error("Auth file " + oneAuth + " is empty");
          }
        } catch (IOException e) {
          logger.error("Error reading auth file " + oneAuth, e);
        }
      } else {
        logger.error("Auth file " + oneAuth + " does not exist");
      }
    } else {
      readCredentials(System.getProperty("user.home") + "/.one/one_auth");
    }
    return null;
  }


  private Client createClient() throws ClientConfigurationException {
    Client provider = new Client(readCredentials(ONE_AUTH), ONE_XMLRPC);
    return provider;
  }

  @Override
  public List<ImageInfoBean> images(String parameters) {
    List<ImageInfoBean> result = new ArrayList<>();
    try {
      ImagePool imgPool = new ImagePool(createClient());
      OneResponse infoRes = imgPool.info();
      if (!infoRes.isError()) {
        Iterator<org.opennebula.client.image.Image> imgResponse = imgPool.iterator();
        Pattern pattern = null;
        if (parameters != null) {
          pattern = Pattern.compile(parameters);
        }
        while (imgResponse != null && imgResponse.hasNext()) {
          org.opennebula.client.image.Image img = imgResponse.next();
          if (pattern == null || (pattern != null && pattern.matcher(img.getName()).matches())) {
            ImageInfoBean imgInfo = new ImageInfoBean();
            imgInfo.setId(img.getId());
            imgInfo.setName(img.getName());
            String templateBase = "TEMPLATE/";
            String dockerId = img.xpath(templateBase + DOCKER_ID);
            if (dockerId != null) {
              imgInfo.setType(ImageInfoBean.ImageType.DOCKER);
              imgInfo.setDockerId(dockerId);
              imgInfo.setDockerName(img.xpath(templateBase + DOCKER_NAME));
              imgInfo.setDockerTag(img.xpath(templateBase + DOCKER_TAG));
            } else {
              imgInfo.setType(ImageInfoBean.ImageType.VM);
            }
            result.add(imgInfo);
          }
        }
      }
    } catch (ClientConfigurationException e) {
      logger.error("Error creating ONE client", e);
    }
    return result;
  }

  @Override
  public ActionResponseBean delete(@PathParam("imageId") String imageId) {
    ActionResponseBean result = new ActionResponseBean();
    try {
      ImagePool client = new ImagePool(createClient());
      OneResponse infoLoad = client.infoAll();
      if (!infoLoad.isError()) {
        org.opennebula.client.image.Image img = client.getById(new Integer(imageId));
        if (img != null) {
          OneResponse response = img.delete();
          result.setSuccess(!response.isError());
          result.setErrorMessage(response.getErrorMessage());
        } else {
          result.setSuccess(false);
          result.setErrorMessage("Image " + imageId + " not found");
        }
      } else {
        result.setSuccess(false);
        result.setErrorMessage("Error loading images info: " + infoLoad.getErrorMessage());
      }
    } catch (ClientConfigurationException e) {
      logger.error("Error deleting image " + imageId, e);
      result.setSuccess(false);
      result.setErrorMessage("OneClient configuration error: " + e.getMessage());
    } catch (NumberFormatException e) {
      result.setErrorMessage("Invalid image id " + imageId + ". A number was expected");
      result.setSuccess(false);
    }

    return result;
  }

  @Override
  public String log(String parameters) {
    return null;
  }

  @Override
  public String space() {
    return null;
  }

  @Override
  public String external() {
    return null;
  }

  @Override
  public String externalSearch(@PathParam("repoId") String repoId) {
    return null;
  }

  @Override
  public String externalPull(@PathParam("repoId") String repoId,
                             @PathParam("imageId") String imageId) {
    return null;
  }

  private String getImageName(Image image) {
    if (image.getRepoTags() != null && image.getRepoTags().length > 0) {
      return image.getRepoTags()[0].split(":")[0];
    } else {
      return "";
    }
  }


  private Integer getDockerDatastoreId(Client oneClient) {
    DatastorePool dataPool = new DatastorePool(oneClient);
    OneResponse poolInfo = dataPool.info();
    Iterator<Datastore> poolResponse = dataPool.iterator();
    Integer dockerStoreId = null;
    while (poolResponse != null && poolResponse.hasNext() && dockerStoreId == null) {
      Datastore store = poolResponse.next();
      if (ONEDOCK_DATASTORE_NAME.equals(store.getName())) {
        return store.id();
      }
    }

    return null;
  }

  @Override
  public String sync(List<Image> imageSummaries, DockerClient client) {

    try {
      Client oneClient = createClient();

      Integer dockerStoreId = getDockerDatastoreId(oneClient);

      List<String> existing = new ArrayList<>();
      if (dockerStoreId != null) {

        ImagePool imgPool = new ImagePool(oneClient);
        OneResponse info = imgPool.info();
        System.out.println(info.getMessage());
        Iterator<org.opennebula.client.image.Image> imgResponse = imgPool.iterator();
        while (imgResponse != null && imgResponse.hasNext()) {
          org.opennebula.client.image.Image img = imgResponse.next();
          String datastore = img.xpath("DATASTORE_ID");
          if (datastore != null) {
            if (dockerStoreId.equals(Integer.parseInt(datastore))) {
              String imgName = img.xpath("PATH");
              if (imgName != null && imgName.startsWith(DOCKER_PREFIX)) {
                existing.add(imgName.substring(DOCKER_PREFIX.length()));
              }
            }
          }
        }


        for (Image image : imageSummaries) {
          String name = getImageName(image);

        }
                /*List<String> toAdd = imageSummaries.stream().
                        map(image -> getImageName(image))
                        .filter(
                            imageName ->
                                !existing.contains(imageName)).collect(Collectors.toList());

                final Integer storeId = dockerStoreId;
                toAdd.forEach(imageName -> addImage(imageName, oneClient, storeId));*/

      }
    } catch (ClientConfigurationException e) {
      e.printStackTrace();
    }


    return null;

  }

  @Override
  public ImageInfoBean imageUpdated(String imageName, String tag,
                                    InspectImageResponse img, DockerClient client) {
    try {
      List<ImageInfoBean> images = images(null);
      boolean create = true;
      Client oneClient = createClient();
      ImagePool imgClient = new ImagePool(oneClient);
      for (ImageInfoBean oneImage : images) {
        if (oneImage.getType().equals(ImageInfoBean.ImageType.DOCKER)
                && oneImage.getDockerName().equals(imageName)
                && oneImage.getDockerTag().equals(tag)) {
          if (img.getId().equals(oneImage.getDockerId())) {
            return oneImage;
          } else {

            org.opennebula.client.image.Image realImage = imgClient.getById(
                    new Integer(oneImage.getId()));

            if (realImage != null) {
              OneResponse deleteResponse = realImage.delete();
              if (deleteResponse.isError()) {
                logger.error("Error deleting obsolete image " + realImage.getId() + ": "
                        + deleteResponse.getErrorMessage());
                create = false;
              }
            }
          }
        }
      }

      if (create) {
        return addImage(imageName, tag, img, oneClient);
      }
    } catch (ClientConfigurationException e) {
      logger.error("Error clreating OpenNebula client", e);
    }
    return null;
  }

  private ImageInfoBean addImage(String imageName, String tag,
                                 InspectImageResponse img, Client oneClient) {

    Integer dockerStoreId = getDockerDatastoreId(oneClient);

    if (dockerStoreId != null) {
      String name = imageName.replace("/", "_");
      String template = new TemplateGenerator()
              .addProperty("NAME", "\"" + name + "\"")
              .addProperty("PATH", DOCKER_PREFIX + imageName + ":" + tag)
              .addProperty("TYPE", "OS")
              .addProperty("DESCRIPTION", "\"Docker image\"")
              .addProperty(DOCKER_ID, img.getId())
              .addProperty(DOCKER_NAME, imageName)
              .addProperty(DOCKER_TAG, tag)
              .generate();

      OneResponse result = org.opennebula.client.image.Image.allocate(
              oneClient, template, dockerStoreId);

      if (result.isError()) {
        logger.error(result.getErrorMessage());
      } else {
        return new ImageInfoBean();
      }
    } else {
      logger.error("Can't find docker datastore id");
    }

    return null;
  }

  private class TemplateGenerator {
    StringBuilder builder = new StringBuilder();

    public TemplateGenerator addProperty(String name, String value) {
      builder.append(name);
      builder.append("=");
      builder.append(value);
      builder.append("\n");
      return this;
    }

    public String generate() {
      return builder.toString();
    }
  }


}
