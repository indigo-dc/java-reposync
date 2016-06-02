package com.atos.indigo.reposync;

import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import dnl.utils.text.table.TextTable;

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.client.ClientConfig;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 * Created by jose on 31/05/16.
 */
public class ReposyncClient {

  private static WebTarget target;

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
    Client client = ClientBuilder.newClient(clientConfig);

    target = client.target(System.getProperty(ReposyncTags.REPOSYNC_REST_ENDPOINT)).path("v1.0");

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

            WebTarget pullTarget = target.path("images").path(args[1]);
            if (args.length > 2) {
              pullTarget = target.queryParam("tag",args[2]);
              progressMsg += " with tag " + args[2];
            }

            System.out.println(progressMsg);

            Response response = pullTarget.request()
                .header(ReposyncTags.TOKEN_HEADER,
                  ConfigurationManager.getProperty(ReposyncTags.REPOSYNC_TOKEN))
                .put(Entity.json("{}"));

            ChunkedInput<ImageInfoBean> input = response
                .readEntity(new GenericType<ChunkedInput<ImageInfoBean>>(){});

            ImageInfoBean imageInfoBean = input.read();

            printImageInfo(imageInfoBean);
            printImageList();
          }
          break;

        case "delete":
          if (args.length < 2) {
            showUsage();
          } else {

            System.out.println("Deleting image with id " + args[1]);

            ActionResponseBean response = target.path("images").path(args[1])
                .request()
                .header(ReposyncTags.TOKEN_HEADER,
                  ConfigurationManager.getProperty(ReposyncTags.REPOSYNC_TOKEN))
                .delete(ActionResponseBean.class);
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

          Response response = target.path("sync").request()
              .header(ReposyncTags.TOKEN_HEADER,
                ConfigurationManager.getProperty(ReposyncTags.REPOSYNC_TOKEN))
              .accept(MediaType.APPLICATION_JSON_TYPE)
              .put(Entity.text(""));

          ChunkedInput<String> input = response
              .readEntity(new GenericType<ChunkedInput<String>>(){});

          /* Chunked input does not work for JSON object. It returns a list of objects one after
           * another which results in just the first object being read. The only solution so far
           * seems to be adding a custom separator between objects and then parsing the chunks
           * separated by that. Since the } character is part of the separator (for safety reasons
           * since a && element could be found inside a JSON object), we need to add it later
           * when we want to parse it to a POJO.
           */
          input.setParser(ChunkedInput.createParser("}&&"));

          ObjectMapper mapper = new ObjectMapper();
          String img = null;
          while ((img = input.read()) != null) {
            try {
              printImageInfo(mapper.readValue(img + "}",ImageInfoBean.class));
            } catch (IOException e) {
              e.printStackTrace();
            }
          }

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
    List<ImageInfoBean> images = target.path("images").request()
        .header(ReposyncTags.TOKEN_HEADER,
          ConfigurationManager.getProperty(ReposyncTags.REPOSYNC_TOKEN))
        .get(new GenericType<List<ImageInfoBean>>(){});

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
