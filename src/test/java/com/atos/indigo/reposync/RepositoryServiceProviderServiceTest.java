package com.atos.indigo.reposync;

import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.command.PullImageResultCallback;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.client.ChunkedInput;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

public class RepositoryServiceProviderServiceTest {

  private HttpServer server;
  private WebTarget target;

  private Map<String, List<InspectImageResponse>> repositories = new HashMap<>();

  private MockRepositoryServiceProvider provider = new MockRepositoryServiceProvider();

  private void createImage(String repo, String id, String[] names) {

    List<InspectImageResponse> repoImgs = repositories.get(repo);
    if (repoImgs == null) {
      repoImgs = new ArrayList<>();
      repositories.put(repo,repoImgs);
    }

    InspectImageResponse img = Mockito.mock(InspectImageResponse.class);

    Mockito.when(img.getId()).thenReturn(id);
    Mockito.when(img.getRepoTags()).thenReturn(Arrays.asList(names));

    repoImgs.add(img);
  }

  @Before
  public void setUp() throws Exception {

    createImage("android", "andNew",new String[]{
      "android:marshmallow",
      "android:6.0",
      "android:latest"
    });

    createImage("android", "andOld",new String[]{
      "android:lollipop",
      "android:5.0"
    });

    createImage("debian", "ubuntuOld",new String[]{
      "debian:weezy",
      "debian:7"
    });

    createImage("debian", "debianNew",new String[]{
      "debian:jessie",
      "debian:8",
      "debian:latest"
    });

    DockerClient mockClient = Mockito.mock(DockerClient.class);

    PullImageCmd mockPullImageCmd = Mockito.mock(PullImageCmd.class);
    PullImageResultCallback mockResultCallback = Mockito.mock(PullImageResultCallback.class);
    Mockito.when(mockPullImageCmd.exec(Matchers.any(ResultCallback.class))).thenReturn(mockResultCallback);
    Mockito.when(mockClient.pullImageCmd(Matchers.anyString())).thenReturn(mockPullImageCmd);

    Mockito.when(mockClient.listImagesCmd()).thenAnswer(new Answer<ListImagesCmd>() {

      private String filterName;

      @Override
      public ListImagesCmd answer(InvocationOnMock invocationOnMock) throws Throwable {
        final ListImagesCmd listImgs = Mockito.mock(ListImagesCmd.class);

        Mockito.when(listImgs.withImageNameFilter(Matchers.anyString())).thenAnswer(new Answer<ListImagesCmd>() {

          @Override
          public ListImagesCmd answer(InvocationOnMock invocationOnMock) throws Throwable {
            filterName = (String) invocationOnMock.getArguments()[0];
            return listImgs;
          }
        });

        Mockito.when(listImgs.exec()).thenAnswer(new Answer<List<Image>>() {
          @Override
          public List<Image> answer(InvocationOnMock invocationOnMock) throws Throwable {
            List<Image> listImgs = new ArrayList<Image>();

            for (Map.Entry<String, List<InspectImageResponse>> entry : repositories.entrySet()) {
              List<InspectImageResponse> imgs = entry.getValue();
              for (InspectImageResponse imgInfo : imgs) {
                if (filterName == null) {
                  listImgs.add(getImageResume(imgInfo));
                } else {
                  List<String> tags = imgInfo.getRepoTags();
                  for (String tag : tags) {
                    if (filterName.equals(tag.split(":")[0])) {
                      listImgs.add(getImageResume(imgInfo));
                    }
                  }
                }

              }
            }

            return listImgs;
          }

          private Image getImageResume(InspectImageResponse imgInfo) {

            Image img = Mockito.mock(Image.class);

            Mockito.when(img.getId()).thenReturn(imgInfo.getId());
            Mockito.when(img.getRepoTags()).thenReturn(imgInfo.getRepoTags().toArray(new String[]{}));

            return img;

          }
        });

        return listImgs;
      }
    });

    Mockito.when(mockClient.inspectImageCmd(Matchers.anyString())).thenAnswer(new Answer<InspectImageCmd>() {

      @Override
      public InspectImageCmd answer(InvocationOnMock invocationOnMock) throws Throwable {
        String imageName = (String) invocationOnMock.getArguments()[0];

        InspectImageCmd operation = Mockito.mock(InspectImageCmd.class);

        Mockito.when(operation.exec()).thenAnswer(new Answer<InspectImageResponse>() {
          @Override
          public InspectImageResponse answer(InvocationOnMock invocationOnMock) throws Throwable {

            for (Map.Entry<String, List<InspectImageResponse>> entry : repositories.entrySet()) {
              for (InspectImageResponse img : entry.getValue()) {
                if (img.getId().equals(invocationOnMock.getArguments()[0])) {
                  return img;
                }
              }
            }
            return null;
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

  @Test
  public void testNotify() {
    String payload = "{\n" +
      "  \"callback_url\": \"https://registry.hub.docker.com/u/getaceres/busybox/hook/2ehdja2ijdhii4j33cbeg134ed3104i33/\", \n" +
      "  \"push_data\": {\n" +
      "    \"images\": [], \n" +
      "    \"pushed_at\": 1463654532, \n" +
      "    \"pusher\": \"testuser\", \n" +
      "    \"tag\": \"6.0\"\n" +
      "  }, \n" +
      "  \"repository\": {\n" +
      "    \"comment_count\": 0, \n" +
      "    \"date_created\": 1463648953, \n" +
      "    \"description\": \"Images of android for unit testing\", \n" +
      "    \"full_description\": \"\", \n" +
      "    \"is_official\": false, \n" +
      "    \"is_private\": false, \n" +
      "    \"is_trusted\": false, \n" +
      "    \"name\": \"android\", \n" +
      "    \"namespace\": \"android\", \n" +
      "    \"owner\": \"testuser\", \n" +
      "    \"repo_name\": \"android\", \n" +
      "    \"repo_url\": \"https://hub.docker.com/r/android\", \n" +
      "    \"star_count\": 0, \n" +
      "    \"status\": \"Active\"\n" +
      "  }\n" +
      "}";

    System.setProperty(ReposyncTags.REPOSYNC_TOKEN,"test");

    target.path("notify").queryParam("token","falseToken").request().async()
      .post(Entity.json(payload), new InvocationCallback<ImageInfoBean>() {
        @Override
        public void completed(ImageInfoBean imageInfoBean) {
          assert(false);
        }

        @Override
        public void failed(Throwable throwable) {
          assert(throwable instanceof NotAuthorizedException);
        }
      });

    target.path("notify").queryParam("token","test").request().async()
      .post(Entity.json(payload), new InvocationCallback<ImageInfoBean>() {
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

  @Test
  public void testSync() {

    System.setProperty(ReposyncTags.REPOSYNC_REPO_LIST,"[android,debian]");

    int initialSize = provider.getImages().size();

    target.path("sync").request().async().put(Entity.text(""),
      new InvocationCallback<ChunkedInput<InspectImageResponse>>() {

        int expected = 4;

      @Override
      public void completed(ChunkedInput<InspectImageResponse> inspectImageResponse) {
        while (inspectImageResponse.read() != null) {
          assert(provider.getImages().size() == (initialSize + (5-expected)));
          expected --;
        }

        assert (provider.getImages().size() == 9);
      }

      @Override
      public void failed(Throwable throwable) {
        assert(false);
      }
    });

  }

}
