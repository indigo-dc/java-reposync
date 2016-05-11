package com.atos.indigo.reposync;

import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.atos.indigo.reposync.providers.OpenStackRepositoryServiceProvider;
import com.atos.indigo.reposync.providers.RepositoryServiceProvider;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.glassfish.jersey.server.ChunkedOutput;

import javax.inject.Singleton;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("v1.0")
@Singleton
public class RepositoryServiceProviderService {

    public static final String TOKEN = "X-Auth-Token";

    RepositoryServiceProvider provider = new OpenStackRepositoryServiceProvider();
    private JsonBuilderFactory factory = Json.createBuilderFactory(null);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder().withDockerHost("unix:///var/run/docker.sock").withDockerTlsVerify(false).build();
    DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();


    @GET
    @Path("images")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<ImageInfoBean> images(@QueryParam("filter")String filter) {
        return provider.images(filter);
    }


    @PUT
    @Path("images/{imageName}")
    @Produces(MediaType.APPLICATION_JSON)
    public ChunkedOutput<ImageInfoBean> pull(
                                    @HeaderParam(TOKEN) String token,
                                    @PathParam("imageName") String imageName,
                                    @QueryParam("tag")String tag) {

        String systemToken = System.getProperty(TOKEN);
        if (systemToken.equals(token)) {

            final ChunkedOutput<ImageInfoBean> output = new ChunkedOutput<>(ImageInfoBean.class);

            PullImageCmd pullCmd = dockerClient.pullImageCmd(imageName);
            if (tag != null) {
                pullCmd.withTag(tag);
            }

            new Thread() {
                @Override
                public void run() {
                    try {
                        pullCmd.exec(new PullImageResultCallback()).awaitSuccess();
                        InspectImageResponse img = dockerClient.inspectImageCmd(imageName).exec();
                        if (img != null) {
                            output.write(provider.imageUpdated(img, dockerClient));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }.start();

            return output;
        } else {
            throw new NotAuthorizedException("Invalid API token");
        }
    }

    @DELETE
    @Path("images/{imageId}")
    @Produces(MediaType.APPLICATION_JSON)
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
    public String notify(JsonObject payload) {
        System.out.println("payload = [" + payload.toString() + "]");
        return "";
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sync")
    public String sync() {
        return provider.sync(dockerClient.listImagesCmd().exec(), dockerClient);
    }

}
