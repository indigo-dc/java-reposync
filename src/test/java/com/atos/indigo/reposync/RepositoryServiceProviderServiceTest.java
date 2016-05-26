package com.atos.indigo.reposync;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

public class RepositoryServiceProviderServiceTest {

  private HttpServer server;
  private WebTarget target;

  @Before
  public void setUp() throws Exception {
    // start the server
    server = Main.startServer();
    // create the client
    Client c = ClientBuilder.newClient();

    // uncomment the following line if you want to enable
    // support for JSON in the client (you also have to uncomment
    // dependency on jersey-media-json module in pom.xml and Main.startServer())
    // --
    // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());

    target = c.target(System.getProperty(ReposyncTags.REPOSYNC_REST_ENDPOINT));
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void testList() {

    

  }

  /**
   * Test to see that the message "Got it!" is sent in the response.
   */
  @Test
  public void testGetIt() {
    //String responseMsg = target.path("v1.0").request().get(String.class);
    //assertEquals("Got it!", responseMsg);
  }
}
