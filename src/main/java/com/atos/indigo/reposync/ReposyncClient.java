package com.atos.indigo.reposync;

import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.atos.indigo.reposync.common.RepositoryServiceProviderServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import dnl.utils.text.table.TextTable;

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.proxy.WebResourceFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;


/**
 * Created by jose on 31/05/16.
 */
public class ReposyncClient {

  private static RepositoryServiceProviderServiceClient proxy;

  private static final String usage = new StringBuilder()
      .append("Usage:\n\n")
      .append("reposync {start|list|pull|delete|sync} <options>\n\n")
      .append("List of options:\n")
      .append("start: none\n")
      .append("list : none\n")
      .append("pull : <image_name> [tag]\n")
      .append("delete : <image_id>\n")
      .append("sync : none\n")
      .toString();

  /**
   * Returns a list of images installed in the IaaS platform.
   * @return List of images.
   */
  public static List<ImageInfoBean> imageList() {
    return proxy.images(null);
  }

  /**
   * Pulls an external image into the IaaS local platform.
   * @param imageName Image to pull.
   * @param tag Optional tag.
   * @return Information about the new image.
   */
  public static ImageInfoBean pull(String imageName, String tag) {

    ChunkedInput<ImageInfoBean> input = proxy.pull(imageName, tag, "{}");

    return input.read();
  }

  /**
   * Deletes an image from the local IaaS platform.
   * @param imageId Image Id to delete.
   * @return State of the operation.
   */
  public static ActionResponseBean delete(String imageId) {
    return proxy.delete(imageId);
  }

  /**
   * Synchronize the list of repositories configured in repolist file.
   * @param func Function that will be executed every time a successful pull is done.
   */
  public static void sync(Consumer<ImageInfoBean> func) {

    ChunkedInput<String> input = proxy.sync("{}");

    /* Chunked input does not work for JSON object. It returns a list of objects one after
     * another which results in just the first object being read. The only solution so far
     * seems to be adding a custom separator between objects and then parsing the chunks
     * separated by that. Since the } character is part of the separator (for safety reasons
     * since a && element could be found inside a JSON object), we need to add it later
     * when we want to parse it to a POJO.
     */
    input.setParser(ChunkedInput.createParser("}"
        + RepositoryServiceProviderServiceImpl.IMG_SEPARATOR));

    ObjectMapper mapper = new ObjectMapper();
    String img = null;
    while ((img = input.read()) != null) {
      if ("{".equals(img)) {
        System.out.println(
            "No images synchronized. Check if the repository list file is empty");
      } else {
        try {
          ImageInfoBean imgBean = mapper.readValue(img + "}", ImageInfoBean.class);
          func.accept(imgBean);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Main entry point for reposync client utility.
   * @param args Arguments passed by command line.
   */
  public static void main(String[] args) {

    try {
      ConfigurationManager.loadConfig();
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }

    ClientConfig clientConfig = new ClientConfig();
    clientConfig.register(JacksonJsonProvider.class);

    ClientBuilder builder = ClientBuilder.newBuilder();

    Client client = builder.newClient(clientConfig);

    WebTarget target = client.target(
        ConfigurationManager.getProperty(ReposyncTags.REPOSYNC_REST_ENDPOINT));

    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

    List<Object> tokenList = new ArrayList<>();
    tokenList.add(ConfigurationManager.getProperty(ReposyncTags.REPOSYNC_TOKEN));

    headers.put(ReposyncTags.TOKEN_HEADER, tokenList);

    proxy = WebResourceFactory.newResource(RepositoryServiceProviderServiceClient.class,
        target, false, headers, new ArrayList<>(), new Form());

    if (args.length == 0) {
      showUsage();
    } else {
      switch (args[0]) {
        case "list":
          printImageList();
          break;

        case "pull":
          if (args.length < 2) {
            showUsage();
            break;
          } else {
            String progressMsg = "Adding image " + args[1];

            String name = args[1];
            String tag = null;

            if (args.length > 2) {
              tag = args[2];
              progressMsg += " with tag " + args[2];
            }

            System.out.println(progressMsg);

            ImageInfoBean imageInfoBean = pull(name,tag);

            printImageInfo(imageInfoBean);
            printImageList();
          }
          break;

        case "delete":
          if (args.length < 2) {
            showUsage();
          } else {

            System.out.println("Deleting image with id " + args[1]);

            ActionResponseBean response = delete(args[1]);
            if (response.isSuccess()) {
              System.out.println("Successfully deleted image with id " + args[1]);
              printImageList();
            } else {
              System.out.println("Error deleting image with id " + args[1] + ": "
                  + response.getErrorMessage());
            }
          }
          break;

        case "sync":

          System.out.println("Synchronizing images in registries: "
              + ConfigurationManager.getProperty(ReposyncTags.REPOSYNC_REPO_LIST));

          sync(img -> printImageInfo(img));
          printImageList();
          break;

        default:
          showUsage();
          break;
      }
    }
  }

  private static void printImageInfo(ImageInfoBean imageInfoBean) {
    System.out.println("Added new image: ");
    System.out.println("Id: " + imageInfoBean.getId());
    System.out.println("Name: " + imageInfoBean.getName());
    System.out.println("Docker Id: " + imageInfoBean.getDockerId());
    System.out.println("Docker Name: " + imageInfoBean.getDockerName());
    System.out.println("Docker Tag: " + imageInfoBean.getDockerTag());
    System.out.println("\n");
  }

  private static void printImageList() {
    List<ImageInfoBean> images = imageList();

    String[] columns = new String[]{"Id", "Name", "Type",
      "Docker Id", "Docker Name", "Docker Tag"};

    Object[][] data = new Object[images.size()][columns.length];
    for (int i = 0; i < images.size(); i++) {
      ImageInfoBean img = images.get(i);
      Object[] imgData = new Object[columns.length];
      imgData[0] = img.getId();
      imgData[1] = img.getName();
      imgData[2] = img.getType();
      imgData[3] = StringUtils.defaultIfBlank(img.getDockerId(),"--");
      imgData[4] = StringUtils.defaultIfBlank(img.getDockerName(),"--");
      imgData[5] = StringUtils.defaultIfBlank(img.getDockerTag(),"--");
      data[i] = imgData;
    }

    TextTable table = new TextTable(columns, data);
    System.out.println("Image list: ");
    table.printTable();
  }

  private static void showUsage() {

    System.out.print(usage);
  }

}
