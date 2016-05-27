package com.atos.indigo.reposync;

import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.core.command.PullImageResultCallback;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

public class RepositoryServiceProviderServiceTest {

  private HttpServer server;
  private WebTarget target;

  private List<InspectImageResponse> dockerImages = new ArrayList<>();

  private MockRepositoryServiceProvider provider = new MockRepositoryServiceProvider();

  private InspectImageResponse createImage(String id, String[] names) {
    InspectImageResponse img = Mockito.mock(InspectImageResponse.class);

    Mockito.when(img.getId()).thenReturn(id);
    Mockito.when(img.getRepoTags()).thenReturn(Arrays.asList(names));

    return img;
  }

  @Before
  public void setUp() throws Exception {

    dockerImages.add(createImage("andId",new String[]{
      "android:marshmallow",
      "android:6.0"
    }));

    DockerClient mockClient = Mockito.mock(DockerClient.class);

    PullImageCmd mockPullImageCmd = Mockito.mock(PullImageCmd.class);
    PullImageResultCallback mockResultCallback = Mockito.mock(PullImageResultCallback.class);
    Mockito.when(mockPullImageCmd.exec(Matchers.any(ResultCallback.class))).thenReturn(mockResultCallback);
    Mockito.when(mockClient.pullImageCmd(Matchers.anyString())).thenReturn(mockPullImageCmd);

    Mockito.when(mockClient.inspectImageCmd(Matchers.anyString())).thenAnswer(new Answer<InspectImageCmd>() {
      @Override
      public InspectImageCmd answer(InvocationOnMock invocationOnMock) throws Throwable {
        String imageName = (String) invocationOnMock.getArguments()[0];

        InspectImageCmd operation = Mockito.mock(InspectImageCmd.class);
        Mockito.when(operation.exec()).thenAnswer(new Answer<InspectImageResponse>() {
          @Override
          public InspectImageResponse answer(InvocationOnMock invocationOnMock) throws Throwable {
            Optional<InspectImageResponse> resLookup =dockerImages.stream().filter(image ->
              !image.getRepoTags().stream().filter(
                tag -> tag.split(":")[0].equals(imageName)).collect(Collectors.toList()).isEmpty())
              .findFirst();

            if (resLookup.isPresent()) {
              return resLookup.get();
            } else {
              return null;
            }
          }
        });

        return operation;

      }
    });


    RepositoryServiceProviderService svc =
      new RepositoryServiceProviderService(provider,mockClient);

    System.setProperty(ReposyncTags.REPOSYNC_REST_ENDPOINT,"http://localhost:8085");

    // start the server
    server = Main.startServer(svc);
    // create the client
    ClientConfig clientConfig = new ClientConfig();
    clientConfig.register(JacksonJsonProvider.class);
    Client c = ClientBuilder.newClient(clientConfig);

    // uncomment the following line if you want to enable
    // support for JSON in the client (you also have to uncomment
    // dependency on jersey-media-json module in pom.xml and Main.startServer())
    // --
    // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());

    target = c.target(System.getProperty(ReposyncTags.REPOSYNC_REST_ENDPOINT)).path("v1.0");
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void testList() {

    List<ImageInfoBean> images = target.path("images").request()
      .get(new GenericType<List<ImageInfoBean>>(){});

    assert(images.size() == 5);

  }

  @Test
  public void testPull() {

    target.path("images").path("android").queryParam("tag","marshmallow").request().async()
      .put(Entity.text(""), new InvocationCallback<ImageInfoBean>(){

        @Override
        public void completed(ImageInfoBean imageInfoBean) {
          assert(provider.getImages().size() == 6);
        }

        @Override
        public void failed(Throwable throwable) {
          assert(false);
        }
      });


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
