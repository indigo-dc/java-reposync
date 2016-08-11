package com.atos.indigo.reposync.beans;

import com.atos.indigo.reposync.ReposyncTags;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.ContainerConfig;

import java.util.Map;

/**
 * Created by jose on 11/08/16.
 */
public class ImageInfoBeanFactory {

  /**
   * Factory to get standard information from the image inspect.
   * @param imageName Image name.
   * @param imageTag Image tag.
   * @param img The image inspect information.
   * @return A bean with the standarized information for reposync.
   */
  public static ImageInfoBean toDockerBean(String imageName, String imageTag,
                                           InspectImageResponse img) {

    ImageInfoBean result = new ImageInfoBean();

    result.setDockerId(img.getId());
    result.setDockerName(imageName);
    result.setDockerTag(imageTag);
    result.setType(ImageInfoBean.ImageType.DOCKER);

    ContainerConfig config = img.getConfig();
    if (config != null) {
      Map<String, String> labels = config.getLabels();
      if (labels != null) {

        result.setOs(labels.get(ReposyncTags.TYPE_TAG));
        result.setArchitecture(labels.get(ReposyncTags.ARCHITECTURE_TAG));
        result.setDistribution(labels.get(ReposyncTags.DISTRIBUTION_TAG));
        result.setVersion(labels.get(ReposyncTags.DIST_VERSION_TAG));

      }
    }

    return result;
  }

}
