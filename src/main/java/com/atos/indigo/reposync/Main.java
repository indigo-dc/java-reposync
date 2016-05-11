package com.atos.indigo.reposync;

import com.atos.indigo.reposync.providers.OpenStackRepositoryServiceProvider;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Main class.
 *
 */
public class Main {
    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI = "http://localhost:8085/";

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {
        // create a resource config that scans for JAX-RS resources and providers
        // in com.atos.indigo.reposync package
        final ResourceConfig rc = new ResourceConfig().packages("com.atos.indigo.reposync");
        rc.register(AuthorizationRequestFilter.class);
        System.setProperty(RepositoryServiceProviderService.TOKEN, "test");



        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static void execServer() throws IOException {
        final HttpServer server = startServer();
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
        System.in.read();
        server.stop();
    }

    public static void getImages() {

        DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder().withDockerHost("unix:///var/run/docker.sock").withDockerTlsVerify(false).build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        List<Image> listImages = dockerClient.listImagesCmd().exec();
        for (Image img : listImages) {
            InspectImageResponse imageData = dockerClient.inspectImageCmd(img.getId()).exec();
            System.out.println(imageData.toString());
        }

        OpenStackRepositoryServiceProvider provider = new OpenStackRepositoryServiceProvider();
        //RepositoryServiceProvider provider = new OpenNebulaRepositoryServiceProvider();
        provider.sync(listImages,dockerClient);

    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        execServer();
    }
}

