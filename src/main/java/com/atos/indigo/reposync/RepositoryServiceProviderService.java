package com.atos.indigo.reposync;

import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.atos.indigo.reposync.providers.OpenNebulaRepositoryServiceProvider;
import com.atos.indigo.reposync.providers.OpenStackRepositoryServiceProvider;
import com.atos.indigo.reposync.providers.RepositoryServiceProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;

import org.glassfish.jersey.server.ChunkedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("v1.0")
@Singleton
public class RepositoryServiceProviderService {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryServiceProvider.class);

    RepositoryServiceProvider provider =
            (ReposyncTags.REPOSYNC_BACKEND_OS.toLowerCase().equals(
                    System.getProperty(ReposyncTags.REPOSYNC_BACKEND).toLowerCase())) ?
                    new OpenStackRepositoryServiceProvider() :
                    new OpenNebulaRepositoryServiceProvider();
    DockerClient dockerClient = DockerClientBuilder.getInstance().build();

    @GET
    @Path("images")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Authorized
    public List<ImageInfoBean> images(@QueryParam("filter") String filter) {
        return provider.images(filter);
    }


    @PUT
    @Path("images/{imageName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Authorized
    public ChunkedOutput<ImageInfoBean> pull(
            @PathParam("imageName") final String imageName,
            @QueryParam("tag") final String tag) {


        final ChunkedOutput<ImageInfoBean> output = new ChunkedOutput<>(ImageInfoBean.class);

        final PullImageCmd pullCmd = dockerClient.pullImageCmd(imageName);

        if (tag != null) {
            pullCmd.withTag(tag);
        }

        final String finalTag = (tag != null) ? tag : "latest";

        new Thread() {
            @Override
            public void run() {
                try {
                    pullCmd.exec(new PullImageResultCallback()).awaitSuccess();
                    InspectImageResponse img = dockerClient.inspectImageCmd(imageName).exec();
                    if (img != null) {
                        output.write(provider.imageUpdated(imageName, finalTag, img, dockerClient));
                    }
                } catch (Exception e) {
                    logger.error("Error pulling image " + imageName + ":" + finalTag, e);
                } finally {
                    try {
                        output.close();
                    } catch (IOException e) {
                        logger.error("Error writing output response while pull operation", e);
                    }
                }

            }
        }.start();

        return output;
    }

    @DELETE
    @Path("images/{imageId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Authorized
    public ActionResponseBean delete(@PathParam("imageId") String imageId) {
        return provider.delete(imageId);
    }


    @GET
    @Path("log")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String log(String parameters) {
        return "Got it!";
    }


    @GET
    @Path("space")
    @Produces(MediaType.APPLICATION_JSON)
    public String space() {
        return "Got it!";
    }


    @GET
    @Path("external")
    @Produces(MediaType.APPLICATION_JSON)
    public String external() {
        return "Got it!";
    }


    @GET
    @Path("external/{repoId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String externalSearch(@PathParam("repoId") String repoId) {
        return "Got it!";
    }


    @GET
    @Path("external/{repoId}/{imageId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String externalPull(@PathParam("repoId") String repoId,
                               @PathParam("imageId") String imageId) {
        return "Got it!";
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("notify")
    public void notify(@QueryParam("token") String token, ObjectNode payload) {
        if (token != null && token.equals(System.getProperty(ReposyncTags.REPOSYNC_TOKEN))) {
            JsonNode pushData = payload.get("push_data");
            String tag = pushData.get("tag").asText();

            JsonNode repoInfo = payload.get("repository");
            String repoName = repoInfo.get("repo_name").asText();

            ChunkedOutput<ImageInfoBean> result = pull(repoName, tag);
        } else {
            throw new NotAuthorizedException("Authorization token needed");
        }
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sync")
    @Authorized
    public String sync() {
        return provider.sync(dockerClient.listImagesCmd().exec(), dockerClient);
    }

}
