package com.atos.indigo.reposync;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;

import java.util.List;

/**
 * Created by jose on 20/04/16.
 */
public interface RepositoryServiceProvider {


    String login(String username, String password);

    String logout(String token);

    String images(String token, String parameters);

    String pull(String imageName);

    String push(String imageData);

    String delete(String imageId);

    String log(String parameters);

    String space();

    String external();

    String externalSearch(String repoId);

    String externalPull(String repoId, String imageId);

    String sync(List<Image> imageSummaries, DockerClient client);
}
