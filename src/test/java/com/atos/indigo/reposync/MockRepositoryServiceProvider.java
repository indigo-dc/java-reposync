package com.atos.indigo.reposync;

import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.atos.indigo.reposync.providers.RepositoryServiceProvider;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by jose on 27/05/16.
 */
public class MockRepositoryServiceProvider implements RepositoryServiceProvider {

  private List<ImageInfoBean> images = new ArrayList<>();

  private int idCounter = 6;

  private ImageInfoBean generateImage(String id, String name, String dockerId,
                                      String dockerName, String dockerTag) {
    ImageInfoBean img = new ImageInfoBean();
    img.setId(id);
    img.setName(name);
    img.setType((dockerId != null)? ImageInfoBean.ImageType.DOCKER: ImageInfoBean.ImageType.VM);
    img.setDockerId(dockerId);
    img.setDockerName(dockerName);
    img.setDockerTag(dockerTag);
    return img;
  }

  public MockRepositoryServiceProvider() {
    images.add(generateImage("1","Ubuntu","docId1","ubuntu","latest"));
    images.add(generateImage("2","Kubuntu","docId2","kubuntu","14.04"));
    images.add(generateImage("3","Busybox","docId3","busybox","latest"));
    images.add(generateImage("4","Mysql_server","docId4","mysql/mysql-server","5.5"));
    images.add(generateImage("5","Red Hat Linux",null,null,null));
  }

  @Override
  public List<ImageInfoBean> images(String parameters) {
    if (parameters != null) {
      final String pattern = parameters.replace("?", ".?").replace("*", ".*?");
      return images.stream().filter(image -> image.getName().matches(pattern))
        .collect(Collectors.toList());
    } else {
      return images;
    }
  }

  @Override
  public ActionResponseBean delete(String imageId) {
    int oldSize = images.size();
    images.removeIf(img -> img.getId().equals(imageId));
    ActionResponseBean response = new ActionResponseBean();
    response.setSuccess(images.size() != oldSize);
    if (!response.isSuccess()) {
      response.setErrorMessage("Can't find image with id "+imageId);
    }
    return response;
  }

  @Override
  public ImageInfoBean imageUpdated(String imageName, String tag, InspectImageResponse img,
                                    DockerClient client) {
    Optional<ImageInfoBean> foundImg = images.stream().filter(image ->
      ImageInfoBean.ImageType.DOCKER.equals(image.getType())
        && imageName.equals(image.getDockerName()) && tag.equals(image.getDockerTag())).findFirst();

    if (foundImg.isPresent()) {
      ImageInfoBean existing = foundImg.get();
      if (img.getId().equals(existing.getDockerId())) {
        return existing;
      } else {
        delete(existing.getId());
      }
    }

    ImageInfoBean newImg = generateImage(Integer.toString(idCounter),
      imageName,img.getId(),imageName,tag);
    images.add(newImg);
    idCounter++;
    return newImg;
  }

  public List<ImageInfoBean> getImages() {
    return images;
  }
}
