package com.atos.indigo.reposync.providers;

import com.atos.indigo.reposync.ConfigurationException;
import com.atos.indigo.reposync.ConfigurationManager;
import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.atos.indigo.reposync.beans.ImageInfoBeanFactory;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;
import org.opennebula.client.OneResponse;
import org.opennebula.client.datastore.Datastore;
import org.opennebula.client.datastore.DatastorePool;
import org.opennebula.client.image.ImagePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

  public static final String OS = "OS";
  public static final String DISTRIBUTION = "DISTRIBUTION";
  public static final String VERSION = "DIST_VERSION";
  public static final String ARCHITECTURE = "HW_ARCHITECTURE";

  private static final Logger logger = LoggerFactory.getLogger(
          OpenNebulaRepositoryServiceProvider.class);
  private static final String ONE_XMLRPC = ConfigurationManager.getProperty("ONE_XMLRPC");
  private static final String ONE_AUTH = ConfigurationManager.getProperty("ONE_AUTH");
  public static final String TEMPLATE_BASE = "TEMPLATE/";

  private Client client;
  private ImagePool imgPool;
  private DatastorePool dataPool;


  /**
   * Default constructor using the system configuration.
   */
  public OpenNebulaRepositoryServiceProvider() throws ConfigurationException {
    try {
      this.client = new Client(readCredentials(ONE_AUTH), ONE_XMLRPC);
      this.imgPool = new ImagePool(client);
      this.dataPool = new DatastorePool(client);
    } catch (ClientConfigurationException e) {
      logger.error("Error getting OpenNebula client", e);
    }
  }

  /**
   * Constructor for testing purposes.
   * @param client Client stub.
   * @param imgPool ImagePool stub.
   * @param dataPool DatastorePool stub.
   */
  public OpenNebulaRepositoryServiceProvider(Client client, ImagePool imgPool,
                                             DatastorePool dataPool) {
    this.client = client;
    this.imgPool = imgPool;
    this.dataPool = dataPool;
  }

  private static String readCredentials(String oneAuth) throws ConfigurationException {
    if (oneAuth != null) {
      File authFile = new File(oneAuth);
      try {
        LineIterator credIterator = FileUtils.lineIterator(authFile);
        if (credIterator.hasNext()) {
          return credIterator.nextLine().trim();
        } else {
          throw new ConfigurationException("Can't find credentials in auth file " + oneAuth);
        }
      } catch (IOException e) {
        throw new ConfigurationException("Error reading auth file " + oneAuth, e);
      }
    } else {
      throw new ConfigurationException("one_auth file location is not defined");
    }
  }

  @Override
  public List<ImageInfoBean> images(String parameters) {
    List<ImageInfoBean> result = new ArrayList<>();
    OneResponse infoRes = imgPool.info();
    if (!infoRes.isError()) {
      Iterator<org.opennebula.client.image.Image> imgResponse = imgPool.iterator();
      while (imgResponse != null && imgResponse.hasNext()) {
        org.opennebula.client.image.Image img = imgResponse.next();
        if (parameters == null || (parameters != null && img.getName().matches(
            parameters.replace("?", ".?").replace("*", ".*?")))) {
          ImageInfoBean imgInfo = new ImageInfoBean();
          imgInfo.setId(img.getId());
          imgInfo.setName(img.getName());
          String dockerId = img.xpath(TEMPLATE_BASE + DOCKER_ID);
          if (dockerId != null) {
            imgInfo.setType(ImageInfoBean.ImageType.DOCKER);
            imgInfo.setDockerId(dockerId);
            imgInfo.setDockerName(img.xpath(TEMPLATE_BASE + DOCKER_NAME));
            imgInfo.setDockerTag(img.xpath(TEMPLATE_BASE + DOCKER_TAG));
            imgInfo.setArchitecture(img.xpath(TEMPLATE_BASE + ARCHITECTURE));
            imgInfo.setOs(img.xpath(TEMPLATE_BASE + OS));
            imgInfo.setDistribution(img.xpath(TEMPLATE_BASE + DISTRIBUTION));
            imgInfo.setVersion(img.xpath(TEMPLATE_BASE + VERSION));
          } else {
            imgInfo.setType(ImageInfoBean.ImageType.VM);
          }
          result.add(imgInfo);
        }
      }
    }
    return result;
  }

  @Override
  public ActionResponseBean delete(@PathParam("imageId") String imageId) {
    ActionResponseBean result = new ActionResponseBean();
    try {
      OneResponse infoLoad = imgPool.infoAll();
      if (!infoLoad.isError()) {
        org.opennebula.client.image.Image img = imgPool.getById(new Integer(imageId));
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
    } catch (NumberFormatException e) {
      result.setErrorMessage("Invalid image id " + imageId + ". A number was expected");
      result.setSuccess(false);
    }

    return result;
  }

  private Integer getDockerDatastoreId() {
    OneResponse poolInfo = dataPool.info();
    if (!poolInfo.isError()) {
      Iterator<Datastore> poolResponse = dataPool.iterator();
      Integer dockerStoreId = null;
      while (poolResponse != null && poolResponse.hasNext() && dockerStoreId == null) {
        Datastore store = poolResponse.next();
        if (ONEDOCK_DATASTORE_NAME.equals(store.getName())) {
          return store.id();
        }
      }
    } else {
      logger.error("Can't retrieve datastore information: " + poolInfo.getErrorMessage());
    }

    return null;
  }

  @Override
  public ImageInfoBean imageUpdated(String imageName, String tag,
                                    InspectImageResponse img, DockerClient client) {
    List<ImageInfoBean> images = images(null);
    boolean create = true;
    for (ImageInfoBean oneImage : images) {
      if (oneImage.getType().equals(ImageInfoBean.ImageType.DOCKER)
              && oneImage.getDockerName().equals(imageName)
              && oneImage.getDockerTag().equals(tag)) {
        if (img.getId().equals(oneImage.getDockerId())) {
          return oneImage;
        } else {

          org.opennebula.client.image.Image realImage = imgPool.getById(
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
      return addImage(imageName, tag, img);
    }

    return null;
  }

  private ImageInfoBean addImage(String imageName, String tag,
                                 InspectImageResponse img) {

    Integer dockerStoreId = getDockerDatastoreId();

    if (dockerStoreId != null) {

      ImageInfoBean bean = ImageInfoBeanFactory.toDockerBean(imageName, tag, img);

      String name = imageName.replace("/", "_");
      TemplateGenerator templateGenerator = new TemplateGenerator()
          .addProperty("NAME", "\"" + name + "\"")
          .addProperty("PATH", DOCKER_PREFIX + imageName + ":" + tag)
          .addProperty("TYPE", "OS")
          .addProperty("DESCRIPTION", "\"Docker image\"")
          .addProperty(DOCKER_ID, img.getId())
          .addProperty(DOCKER_NAME, imageName)
          .addProperty(DOCKER_TAG, tag);

      String os = bean.getOs();
      if (os != null) {
        templateGenerator.addProperty(OS, bean.getOs());
      }

      String arch = bean.getArchitecture();
      if (arch != null) {
        templateGenerator.addProperty(ARCHITECTURE, img.getArch());
      }

      String dist = bean.getDistribution();
      if (dist != null) {
        templateGenerator.addProperty(DISTRIBUTION,dist);
      }

      String version = bean.getVersion();
      if (version != null) {
        templateGenerator.addProperty(VERSION, version);
      }

      String template = templateGenerator
              .generate();

      OneResponse result = org.opennebula.client.image.Image.allocate(
              client, template, dockerStoreId);

      if (result.isError()) {
        logger.error(result.getErrorMessage());
      } else {
        String newId = result.getMessage();
        ImageInfoBean newImg = new ImageInfoBean();
        newImg.setId(newId);
        newImg.setName(name);
        newImg.setType(ImageInfoBean.ImageType.DOCKER);
        newImg.setDockerId(img.getId());
        newImg.setDockerName(imageName);
        newImg.setDockerTag(tag);
        newImg.setArchitecture(img.getArch());
        newImg.setOs(img.getOs());
        newImg.setDistribution(dist);
        newImg.setVersion(version );
        return newImg;
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
