package com.atos.indigo.reposync;

import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.ws.rs.client.WebTarget;


/**
 * Created by jose on 17/06/16.
 */
public class IntegrationTest {

  private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);

  private static WebTarget target;

  private static final String IMG_NAME = "busybox";
  private static final String IMG_TAG = "latest";

  private static boolean findImage(List<ImageInfoBean> imgList, ImageInfoBean bean) {
    for (ImageInfoBean img : imgList) {
      if (bean.getDockerName().equals(img.getDockerName())
          && bean.getDockerTag().equals(img.getDockerTag())
          && bean.getDockerId().equals(img.getDockerId())
          && bean.getId().equals(img.getId())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Execute the integration test.
   * @param args Ignored.
   */
  public static void main(String[] args) {
    try {
      runTest();
      logger.info("Integration test successful");
    } catch (Throwable e) {
      logger.error(e.getMessage());
    }
  }

  private static void runTest() {
    try {
      ConfigurationManager.loadConfig();
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }

    ReposyncClient client = new ReposyncClient();

    List<ImageInfoBean> original = client.imageList();

    for (ImageInfoBean img : original) {
      if (IMG_NAME.equals(img.getDockerName()) && IMG_TAG.equals(img.getDockerTag())) {
        ActionResponseBean response = client.delete(img.getId());
        if (!response.isSuccess()) {
          throw new RuntimeException("Can't delete preexisting " + IMG_NAME + ":"
            + IMG_TAG + " image: ");
        }
        break;
      }
    }

    ImageInfoBean added = client.pull(IMG_NAME, IMG_TAG);
    List<ImageInfoBean> afterPull = client.imageList();

    boolean found = findImage(afterPull, added);

    if (found) {
      ActionResponseBean response = client.delete(added.getId());
      if (response.isSuccess()) {
        found = findImage(client.imageList(), added);

        if (!found) {
          testSynchronization(target);
        } else {
          throw new RuntimeException("Delete image succeed but image " + added.getDockerName()
            + ":" + added.getDockerTag() + " is still present");
        }
      } else {
        throw new RuntimeException("Error deleting image " + added.getDockerName()
          + ":" + added.getDockerTag() + ": " + response.getErrorMessage());
      }
    } else {
      throw new RuntimeException("Test image " + IMG_NAME + ":" + IMG_TAG + " not added");
    }
  }

  /**
   * Execute the synchronization test.
   * @param target REST client.
   */
  private static void testSynchronization(WebTarget target) {

    final List<String> repos = ConfigurationManager.getRepoList();

    List<ImageInfoBean> images = ReposyncClient.imageList();

    for (ImageInfoBean img : images) {
      if (img.getDockerName() != null && repos.contains(img.getDockerName())) {
        ActionResponseBean response = ReposyncClient.delete( img.getId());
        if (!response.isSuccess()) {
          throw new RuntimeException("Error deleting image " + img.getDockerName()
            + ":" + img.getDockerTag() + ": " + response.getErrorMessage());
        }
      }
    }

    ReposyncClient.sync(img -> repos.remove(img.getDockerName()));

    if (repos.isEmpty()) {
      List<String> reposOrig = ConfigurationManager.getRepoList();

      images = ReposyncClient.imageList();
      for (ImageInfoBean img : images) {
        if (img.getDockerName() != null) {
          reposOrig.remove(img.getDockerName());
        }
      }

      if (reposOrig.isEmpty()) {
        System.out.println("Synchronization test successful");
      } else {
        String errorMsg = "Not all images are synchronized. Remaining: ";
        for (String repo : reposOrig) {
          errorMsg += repo + "\n";
        }
        throw new RuntimeException(errorMsg);
      }
    } else {
      String errorMsg = "Not all images are pulled. Remaining: ";
      for (String repo : repos) {
        errorMsg += repo + "\n";
      }
      throw new RuntimeException(errorMsg);
    }

  }

}
