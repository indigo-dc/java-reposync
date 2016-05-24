package com.atos.indigo.reposync.providers;

import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.Image;

import java.util.List;

/**
 * Created by jose on 20/04/16.
 */
public interface RepositoryServiceProvider {


  List<ImageInfoBean> images(String parameters);

  ActionResponseBean delete(String imageId);

  String log(String parameters);

  String space();

  String external();

  String externalSearch(String repoId);

  String externalPull(String repoId, String imageId);

  String sync(List<Image> imageSummaries, DockerClient client);

  ImageInfoBean imageUpdated(String imageName, String tag, InspectImageResponse img, DockerClient client);
}
