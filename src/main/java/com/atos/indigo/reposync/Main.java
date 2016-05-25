package com.atos.indigo.reposync;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

/**
 * Main class.
 */
public class Main {

  public static final String BASE_PKG = "com.atos.indigo.reposync";

  /**
   * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
   *
   * @return Grizzly HTTP server.
   */
  public static HttpServer startServer() throws ConfigurationException {
    // create a resource config that scans for JAX-RS resources and providers
    // in com.atos.indigo.reposync package
    ConfigurationManager.loadConfig();
    final ResourceConfig rc = new ResourceConfig().packages(BASE_PKG);
    rc.register(AuthorizationRequestFilter.class);


    // create and start a new instance of grizzly http server
    // exposing the Jersey application at BASE_URI
    return GrizzlyHttpServerFactory.createHttpServer(
            URI.create(ConfigurationManager.getProperty(ReposyncTags.REPOSYNC_REST_ENDPOINT)), rc);
  }


  public static void execServer() throws IOException, ConfigurationException {
    final HttpServer server = startServer();
    System.out.println(String.format("Jersey app started in Grizzly with WADL available at "
            + "%sapplication.wadl\nHit enter to stop it...", ConfigurationManager.getProperty(
            ReposyncTags.REPOSYNC_REST_ENDPOINT)));
    System.in.read();
    server.stop();
  }

  /**
   * Main method.
   */
  public static void main(String[] args) throws Exception {
    execServer();
  }
}

