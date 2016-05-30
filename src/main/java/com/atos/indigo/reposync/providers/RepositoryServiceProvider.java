package com.atos.indigo.reposync.providers;

import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;

import java.util.List;

/**
 * Created by jose on 20/04/16.
 */
public interface RepositoryServiceProvider {


  List<ImageInfoBean> images(String parameters);

  ActionResponseBean delete(String imageId);

  ImageInfoBean imageUpdated(String imageName, String tag,
                             InspectImageResponse img, DockerClient client);
}
