package com.atos.indigo.reposync.providers;

import com.atos.indigo.reposync.ConfigurationException;
import com.atos.indigo.reposync.ConfigurationManager;
import com.atos.indigo.reposync.ReposyncTags;
import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.atos.indigo.reposync.beans.ImageInfoBeanFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.client.IOSClientBuilder;
import org.openstack4j.api.image.ImageService;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.model.image.ContainerFormat;
import org.openstack4j.model.image.DiskFormat;
import org.openstack4j.model.image.Image;
import org.openstack4j.model.image.builder.ImageBuilder;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jose on 25/04/16.
 */
public class OpenStackRepositoryServiceProvider implements RepositoryServiceProvider {

  private static final Logger logger = LoggerFactory.getLogger(
      OpenStackRepositoryServiceProvider.class);

  public static final String DOCKER_ID = "docker_id";
  public static final String DOCKER_IMAGE_NAME = "docker_name";
  public static final String DOCKER_IMAGE_TAG = "docker_tag";
  private static final String ENDPOINT = ConfigurationManager.getProperty("OS_AUTH_URL");
  private static final String PROJECT_DOMAIN =
          ConfigurationManager.getProperty("OS_PROJECT_DOMAIN_NAME");
  private static final String PROJECT = ConfigurationManager.getProperty("OS_PROJECT_NAME");
  private static final String DOMAIN = ConfigurationManager.getProperty("OS_USER_DOMAIN_NAME");
  private static final String ADMIN_USER_VAR = "OS_USERNAME";
  private static final String ADMIN_PASS_VAR = "OS_PASSWORD";
  private static final String SHARE_CONFIG =
      ConfigurationManager.getProperty(ReposyncTags.SHARE_CONFIG);

  public static final String OS = "os";
  public static final String DISTRIBUTION = "distribution";
  public static final String VERSION = "dist_version";
  public static final String ARCHITECTURE = "hw_architecture";

  private ObjectMapper mapper = new ObjectMapper();
  private Map<String, String[]> shareConfig;

  private Token token;

  /**
   * Default constructor using the client configuration defined in the system.
   * @throws ConfigurationException If the configuration is not found or incorrect.
   */
  public OpenStackRepositoryServiceProvider() throws ConfigurationException {
    OSFactory.enableHttpLoggingFilter(ConfigurationManager.isDebugMode());
    if (SHARE_CONFIG != null) {
      TypeFactory typeFactory = mapper.getTypeFactory();
      MapType mapType = typeFactory.constructMapType(HashMap.class,
          typeFactory.constructType(String.class),
          typeFactory.constructArrayType(String.class));
      try {
        shareConfig = mapper.readValue(SHARE_CONFIG, mapType);
      } catch (IOException e) {
        logger.error("Error reading sharing information from configuration",e);
      }
    }
  }

  public OpenStackRepositoryServiceProvider(Token token) {
    this.token = token;
  }

  private OSClient.OSClientV3 getClient(String username, String password) {
    logger.debug("Getting client from OpenStack4j");
    logger.debug("Creating V3 builder");

    IOSClientBuilder.V3 builder = OSFactory.builderV3();
    logger.debug("Configuring builder: ");
    if (ConfigurationManager.isDebugMode()) {
      logger.debug("Endpoint: " + ENDPOINT);
      logger.debug("Username: " + username);
      logger.debug("Password: " + password);
      logger.debug("Domain: " + DOMAIN);
      logger.debug("Project: " + PROJECT);
      logger.debug("Project domain: " + PROJECT_DOMAIN);
    }
    builder = builder.endpoint(ENDPOINT)
            .credentials(username, password, Identifier.byName(DOMAIN))
            .withConfig(Config.DEFAULT.withSSLVerificationDisabled())
            .scopeToProject(Identifier.byName(PROJECT), Identifier.byName(PROJECT_DOMAIN));
    logger.debug("Authenticating");
    OSClient.OSClientV3 client = builder.authenticate();
    logger.debug("Getting new client's token");
    token = client.getToken();
    logger.debug("Got client's token");
    return client;
  }

  private ImageService getAdminClient() {
    logger.debug("Getting OpenStack client");
    try {
      if (token == null || hasExpired(token)) {
        logger.debug("No existing token found, creating new session");
        String adminUser = ConfigurationManager.getProperty(ADMIN_USER_VAR);
        String adminPass = ConfigurationManager.getProperty(ADMIN_PASS_VAR);

        if (adminUser != null && adminPass != null) {
          logger.debug("Got usename and password from configuration, getting client");
          return getClient(adminUser, adminPass).images();
        } else {
          throw new ConfigurationException("Openstack user and password are mandatory.");
        }
      } else {
        return OSFactory.clientFromToken(token).images();
      }
    } catch (ConfigurationException e) {
      logger.error("Invalid configuration found for Open Stack",e);
      return null;
    }
  }

  private boolean hasExpired(Token token) {
    Date now = new Date();
    return now.after(token.getExpires());
  }

  @Override
  public List<ImageInfoBean> images(String filter) {
    List<ImageInfoBean> result = new ArrayList<>();
    logger.debug("Listing OpenStack images");
    List<? extends Image> images = getAdminClient().listAll();
    if (images != null) {
      for (Image img : images) {
        if ((filter != null && img.getName().matches(
            filter.replace("?", ".?").replace("*", ".*?")))
            || filter == null) {
          result.add(getImageInfo(img));
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
        result.setArchitecture(imgProps.get(ARCHITECTURE));
        result.setOs(imgProps.get(OS));
        result.setDistribution(imgProps.get(DISTRIBUTION));
        result.setVersion(imgProps.get(VERSION));
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
    ActionResponse opResult = getAdminClient().delete(imageId);
    ActionResponseBean result = new ActionResponseBean();
    result.setSuccess(opResult.isSuccess());
    result.setErrorMessage(opResult.getFault());
    return result;
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
  public ImageInfoBean imageUpdated(String imageName, String tag, InspectImageResponse img,
                                    DockerClient restClient) {
    ImageService api = getAdminClient();
    ImageInfoBean foundImg = findImage(imageName, tag, api.listAll());
    if (foundImg != null) {
      if (!img.getId().equals(foundImg.getDockerId())) {
        ActionResponse response = getAdminClient().delete(foundImg.getId());
        if (!response.isSuccess()) {
          logger.error("Error deleting updated image: " + response.getFault());
          return null;
        }
      } else {
        return foundImg;
      }
    }

    try {
      return getImageInfo(addImage(img, imageName, tag, img.getId(), api, restClient));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private Image addImage(InspectImageResponse img, String name, String tag, String id,
                         ImageService api, DockerClient restClient) throws IOException {

    InputStream responseStream = restClient.saveImageCmd(id).exec();
    if (responseStream != null) {

      ImageInfoBean bean = ImageInfoBeanFactory.toDockerBean(name, tag, img);


      ImageBuilder imageBuilder = Builders.image().name(name)
          .containerFormat(ContainerFormat.DOCKER)
          .diskFormat(DiskFormat.RAW)
          .property(DOCKER_ID, id)
          .property(DOCKER_IMAGE_NAME, name)
          .property(DOCKER_IMAGE_TAG, tag)
          .property("img_hv_type", "docker")
          .property("hw_vm_mode", "exe")
          .property("hypervisor_type", "docker");

      String os = bean.getOs();
      if (os != null) {
        imageBuilder.property(OS, os);
      }

      String arch = bean.getArchitecture();
      if (arch != null) {
        imageBuilder.property(ARCHITECTURE, arch);
      }

      String dist = bean.getDistribution();
      if (dist != null) {
        imageBuilder.property(DISTRIBUTION,dist);
      }

      String version = bean.getVersion();
      if (version != null) {
        imageBuilder.property(VERSION, version);
      }

      org.openstack4j.model.common.Payload payload = Payloads.create(responseStream);

      Image result = api.create(imageBuilder.build(), payload);

      if (shareConfig != null) {
        shareImage(result, name, tag, api);
      }

      return result;
    } else {
      return null;
    }

  }

  private void shareImage(Image result, String name, String tag, ImageService api) {
    String[] tenants = shareConfig.get(name);
    if (tenants != null) {
      addTenants(result.getId(), tenants, api);
    } else {
      tenants = shareConfig.get(name + ":" + tag);
      if (tenants != null) {
        addTenants(result.getId(), tenants, api);
      }
    }
  }

  private void addTenants(String imageId, String[] tenants, ImageService api) {
    for (String tenant : tenants) {
      try {
        api.addMember(imageId, tenant);
      } catch (Exception e) {
        logger.error("Error sharing image " + imageId + " with tenant " + tenant, e);
      }
    }
  }
}
