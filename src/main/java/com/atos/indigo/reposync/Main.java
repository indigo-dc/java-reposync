package com.atos.indigo.reposync;

import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * Main class.
 */
public class Main implements Daemon{

  public static final String BASE_PKG = "com.atos.indigo.reposync";
  private static final Logger logger = LoggerFactory.getLogger(Main.class);
  private static HttpServer server;

  private static SSLEngineConfigurator getSslConfiguration(boolean secure) {
    SSLContextConfigurator configurator = new SSLContextConfigurator();

    if (secure) {
      configurator.setKeyStoreFile(
          ConfigurationManager.getProperty(ReposyncTags.KEYSTORE_LOCATION));

      configurator.setKeyStorePass(
          ConfigurationManager.getProperty(ReposyncTags.KEYSTORE_PASSWORD));

    }

    return new SSLEngineConfigurator(configurator).setClientMode(false).setNeedClientAuth(false);
  }

  /**
   * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
   *
   * @return Grizzly HTTP server.
   */
  public static HttpServer startServer(RepositoryServiceProviderServiceImpl svc, String url)
    throws ConfigurationException {
    // create a resource config that scans for JAX-RS resources and providers
    // in com.atos.indigo.reposync package
    ResourceConfig rc = new ResourceConfig();
    if (svc == null) {
      rc.register(RepositoryServiceProviderServiceImpl.class);
      rc.register(JacksonFeatures.class);
      rc.register(JacksonJsonProvider.class);
      rc.register(AuthorizationRequestFilter.class);
    } else {
      // For testing purposes
      rc.register(svc);
    }

    String secureProp = ConfigurationManager.getProperty(ReposyncTags.USE_SSL);
    boolean secure = (secureProp != null) ? new Boolean(secureProp) : false;

    // create and start a new instance of grizzly http server
    // exposing the Jersey application at BASE_URI
    return GrizzlyHttpServerFactory.createHttpServer(
            URI.create(url), rc, secure, getSslConfiguration(secure));
  }

  /**
   * Execute a standalone Grizzly server.
   * @throws IOException If the server fails to start.
   * @throws ConfigurationException If some needed properties are not found.
   */
  public static void execServer() throws IOException, ConfigurationException {
    server = startServer(null,
        ConfigurationManager.getProperty(ReposyncTags.REPOSYNC_REST_ENDPOINT));
    System.out.println(String.format("Jersey app started in Grizzly with WADL available at "
            + "%s/application.wadl\n", ConfigurationManager.getProperty(
            ReposyncTags.REPOSYNC_REST_ENDPOINT)));

  }

  /**
   * Main method.
   */
  public static void main(String[] args) throws Exception {
    ConfigurationManager.loadConfig();
    execServer();
  }

  @Override
  public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {
    ConfigurationManager.loadConfig();
  }

  @Override
  public void start() throws Exception {
    execServer();
  }

  @Override
  public void stop() throws Exception {
    server.shutdown();
  }

  @Override
  public void destroy() {

  }
}

