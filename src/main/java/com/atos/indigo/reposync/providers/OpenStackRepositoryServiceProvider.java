package com.atos.indigo.reposync.providers;

import com.atos.indigo.reposync.ConfigurationManager;
import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.image.ImageService;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.image.ContainerFormat;
import org.openstack4j.model.image.DiskFormat;
import org.openstack4j.model.image.Image;
import org.openstack4j.openstack.OSFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by jose on 25/04/16.
 */
public class OpenStackRepositoryServiceProvider implements RepositoryServiceProvider {

  public static final String DOCKER_ID = "dockerid";
  public static final String DOCKER_IMAGE_NAME = "dockername";
  public static final String DOCKER_IMAGE_TAG = "dockertag";
  private static final String ENDPOINT = ConfigurationManager.getProperty("OS_AUTH_URL");
  private static final String PROJECT_DOMAIN =
          ConfigurationManager.getProperty("OS_PROJECT_DOMAIN_NAME");
  private static final String PROJECT = ConfigurationManager.getProperty("OS_PROJECT_NAME");
  private static final String DOMAIN = ConfigurationManager.getProperty("OS_USER_DOMAIN_NAME");
  private static final String ADMIN_USER_VAR = "OS_USERNAME";
  private static final String ADMIN_PASS_VAR = "OS_PASSWORD";
  private ObjectMapper mapper = new ObjectMapper();

  private OSClient getClient(String username, String password) {

    OSClient client = OSFactory.builderV3()
            .endpoint(ENDPOINT)
            .credentials(username, password, Identifier.byName(DOMAIN))
            .withConfig(Config.DEFAULT.withSSLVerificationDisabled())
            .scopeToProject(Identifier.byName(PROJECT), Identifier.byName(PROJECT_DOMAIN))
            .authenticate();
    return client;
  }

  private ImageService getAdminClient() {
    String adminUser = ConfigurationManager.getProperty(ADMIN_USER_VAR);
    String adminPass = ConfigurationManager.getProperty(ADMIN_PASS_VAR);

    if (adminUser != null && adminPass != null) {
      return getClient(adminUser, adminPass).images();
    } else {
      return null;
    }
  }

  @Override
  public List<ImageInfoBean> images(String filter) {
    List<ImageInfoBean> result = new ArrayList<>();
    ImageService api = getAdminClient();
    if (api != null) {
      List<? extends Image> images = api.listAll();
      if (images != null) {
        Pattern pattern = null;
        if (filter != null) {
          pattern = Pattern.compile(filter);
        }
        for (Image img : images) {
          if ((pattern != null && pattern.matcher(img.getName()).matches()) || pattern == null) {
            result.add(getImageInfo(img));
          }
        }
      }
    }
    return result;
  }

  private ImageInfoBean getImageInfo(Image image) {
    if (image != null) {
      ImageInfoBean result = new ImageInfoBean();
      result.setId(image.getId());
      result.setName(image.getName());
      Map<String, String> imgProps = image.getProperties();

      if (imgProps != null && imgProps.get(DOCKER_ID) != null) {
        result.setType(ImageInfoBean.ImageType.DOCKER);
        result.setDockerId(imgProps.get(DOCKER_ID));
        result.setDockerName(imgProps.get(DOCKER_IMAGE_NAME));
        result.setDockerTag(imgProps.get(DOCKER_IMAGE_TAG));
      } else {
        result.setType(ImageInfoBean.ImageType.VM);
      }
      return result;
    } else {
      return null;
    }
  }


  @Override
  public ActionResponseBean delete(String imageId) {
    ImageService client = getAdminClient();
    ActionResponse opResult = client.delete(imageId);
    ActionResponseBean result = new ActionResponseBean();
    result.setSuccess(opResult.isSuccess());
    result.setErrorMessage(opResult.getFault());
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
  public String externalSearch(String repoId) {
    return null;
  }

  @Override
  public String externalPull(String repoId, String imageId) {
    return null;
  }

  private ImageInfoBean findImage(String imageName, String tag, List<? extends Image> imageList) {
    for (Image img : imageList) {
      ImageInfoBean imgInfo = getImageInfo(img);
      if (ImageInfoBean.ImageType.DOCKER.equals(imgInfo.getType())
              && imageName.equals(imgInfo.getDockerName())
              && tag.equals(imgInfo.getDockerTag())) {
        return imgInfo;
      }
    }
    return null;
  }

  @Override
  public String sync(List<com.github.dockerjava.api.model.Image> imageSummaries,
                     DockerClient dockerClient) {
    /*String adminUser = System.getenv(ADMIN_USER_VAR);
    String adminPass = System.getenv(ADMIN_PASS_VAR);

    if (adminUser != null && adminPass != null) {
        ImageService api = getClient(adminUser, adminPass).images();
        List<? extends Image> imageList = api.listAll();

        for (com.github.dockerjava.api.model.Image img : imageSummaries) {
            Image found = findImage(img.getId(), imageList);
            if (found == null) {
                try {
                    addImage(img, api, dockerClient);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }*/

    return null;
  }

  @Override
  public ImageInfoBean imageUpdated(String imageName, String tag, InspectImageResponse img,
                                    DockerClient restClient) {
    ImageService client = getAdminClient();
    if (client != null) {
      ImageInfoBean foundImg = findImage(imageName, tag, client.listAll());
      if (foundImg != null) {
        if (!img.getId().equals(foundImg.getDockerId())) {
          ActionResponse response = client.delete(foundImg.getId());
          if (!response.isSuccess()) {
            System.out.println("Error deleting updated image: " + response.getFault());
            return null;
          }
        } else {
          return foundImg;
        }
      }

      try {
        return getImageInfo(addImage(imageName, tag, img.getId(), client, restClient));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private Image addImage(String name, String tag, String id, ImageService api,
                         DockerClient restClient) throws IOException {

    System.out.println("Adding image " + id);

    InputStream responseStream = restClient.saveImageCmd(id).exec();
    if (responseStream != null) {
      org.openstack4j.model.common.Payload payload = Payloads.create(responseStream);
      Image result = api.create(Builders.image().name(name)
              .containerFormat(ContainerFormat.DOCKER)
              .diskFormat(DiskFormat.RAW)
              .property(DOCKER_ID, id)
              .property(DOCKER_IMAGE_NAME, name)
              .property(DOCKER_IMAGE_TAG, tag)
              .property("hypervisor_type", "docker").build(), payload);
      System.out.println("Created image " + result.getId());
      return result;
    } else {
      return null;
    }

  }
}
