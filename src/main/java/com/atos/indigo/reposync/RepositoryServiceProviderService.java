package com.atos.indigo.reposync;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

import javax.inject.Singleton;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("v1.0")
@Singleton
public class RepositoryServiceProviderService {

    private static final String TOKEN = "X-Auth-Token";

    RepositoryServiceProvider provider = new OpenStackRepositoryServiceProvider();
    private JsonBuilderFactory factory = Json.createBuilderFactory(null);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");


    private String getTokenResponse(String token, Date expiration) {
        return factory.createObjectBuilder()
                .add("access", factory.createObjectBuilder()
                        .add("token", factory.createObjectBuilder()
                                .add("issued_at", dateFormat.format(new Date()))
                                .add("expires", dateFormat.format(expiration))
                                .add("id", token)
                        )
                ).build().toString();
    }

    @GET
    @Path("login")
    @Produces(MediaType.APPLICATION_JSON)
    public String login(@HeaderParam("username") String username,
                                        @HeaderParam("password") String password) {

            if (username != null && password != null) {
                String token = provider.login(username, password);
                Date expiration = AuthorizationManager.add(token);
                return getTokenResponse(token, expiration);
            } else {
                throw new BadRequestException("Username and password are mandatory");
            }

    }


    @GET
    @Path("logout")
    @Produces(MediaType.APPLICATION_JSON)
    public String logout(@HeaderParam(TOKEN)String token) {
        String response = provider.logout(token);
        if (response != null) {
            return getTokenResponse(token, new Date());
        } else {
            throw new NotAuthorizedException("Invalid token");
        }
    }


    @GET
    @Path("images")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String images(@HeaderParam(TOKEN)String token, @QueryParam("filter")String filter) {
        String info = provider.images(token, filter);
        return info;
    }


    @GET
    @Path("images/{imageName}")
    @Produces(MediaType.APPLICATION_JSON)
    public String pull(@PathParam("imageName") String imageName) {
        return "Got it!";
    }


    @POST
    @Path("images")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String push(String imageData) {
        return "Got it!";
    }


    @DELETE
    @Path("images/{imageId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String delete(@PathParam("imageId") String imageId) {
        return "Got it!";
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

        DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder().withDockerHost("unix:///var/run/docker.sock").withDockerTlsVerify(false).build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        return provider.sync(dockerClient.listImagesCmd().exec(), dockerClient);
    }

}
