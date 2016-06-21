package com.atos.indigo.reposync;

import com.atos.indigo.reposync.providers.OpenStackRepositoryServiceProvider;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.SaveImageCmd;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.image.ImageService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.model.image.Image;
import org.openstack4j.openstack.OSFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jose on 26/05/16.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(OSFactory.class)
public class OpenStackProviderTest extends RepositoryServiceProviderTest<OpenStackProviderTest.TestImage> {

  protected interface TestImage extends Image {

  }

  private int idCounter = 6;

  protected TestImage mockImage(final String id, String name, String dockerId,
                                                      String dockerName, String dockerTag) {
    TestImage img = Mockito.mock(TestImage.class);

    Mockito.when(img.getId()).thenReturn(id);
    Mockito.when(img.getName()).thenReturn(name);

    Map<String,String> properties = new HashMap<>();
    properties.put(OpenStackRepositoryServiceProvider.DOCKER_ID,dockerId);
    properties.put(OpenStackRepositoryServiceProvider.DOCKER_IMAGE_NAME,dockerName);
    properties.put(OpenStackRepositoryServiceProvider.DOCKER_IMAGE_TAG,dockerTag);

    Mockito.when(img.getProperties()).thenReturn(properties);

    return img;
  }

  private ActionResponse mockResponse(boolean success, String errorMessage) {
    ActionResponse response = Mockito.mock(ActionResponse.class);

    Mockito.when(response.isSuccess()).thenReturn(success);
    Mockito.when(response.getFault()).thenReturn(errorMessage);

    return response;
  }

  @Before
  public void setUp() {

    ImageService client = Mockito.mock(ImageService.class);

    Mockito.when(client.listAll()).thenAnswer(new Answer<List<? extends Image>>() {
      @Override
      public List<? extends Image> answer(InvocationOnMock invocationOnMock) throws Throwable {
        return imageList;
      }
    });

    Mockito.when(client.delete(Matchers.anyString())).thenAnswer(new Answer<ActionResponse>() {
      @Override
      public ActionResponse answer(InvocationOnMock invocationOnMock) throws Throwable {
        String id = (String) invocationOnMock.getArguments()[0];
        TestImage toDelete = null;
        for (TestImage img : imageList) {
          if (img.getId().equals(id)) {
            toDelete = img;
            break;
          }
        }

        if (toDelete != null) {
          imageList.remove(toDelete);
          return mockResponse(true, null);
        } else {
          return mockResponse(false, "Can't find image with id "+id);
        }
      }
    });

    Mockito.when(client.create(Matchers.any(Image.class),Matchers.any(Payload.class)))
      .thenAnswer(new Answer<Image>() {
        @Override
        public Image answer(InvocationOnMock invocationOnMock) throws Throwable {
          Image img = (Image) invocationOnMock.getArguments()[0];

          Map<String, String> properties = img.getProperties();
          TestImage newImg = mockImage(Integer.toString(idCounter),img.getName(),
            properties.get(OpenStackRepositoryServiceProvider.DOCKER_ID),
            properties.get(OpenStackRepositoryServiceProvider.DOCKER_IMAGE_NAME),
            properties.get(OpenStackRepositoryServiceProvider.DOCKER_IMAGE_TAG));

          imageList.add(newImg);
          idCounter++;
          return newImg;
        }
      });

    Token mockToken = Mockito.mock(Token.class);
    Mockito.when(mockToken.getExpires()).thenAnswer(new Answer<Date>() {
      @Override
      public Date answer(InvocationOnMock invocationOnMock) throws Throwable {
        Date now = new Date();
        now.setTime(now.getTime() + 100000);
        return now;
      }
    });

    PowerMockito.mockStatic(OSFactory.class);

    OSClient.OSClientV3 os = Mockito.mock(OSClient.OSClientV3.class);
    Mockito.when(os.images()).thenReturn(client);


    Mockito.when(OSFactory.clientFromToken(mockToken)).thenReturn(os);

    provider = new OpenStackRepositoryServiceProvider(mockToken);

  }

  @Override
  protected DockerClient mockDockerClient() {
    DockerClient client = Mockito.mock(DockerClient.class);

    Mockito.when(client.saveImageCmd(Matchers.anyString())).thenAnswer(
      new Answer<SaveImageCmd>() {
        @Override
        public SaveImageCmd answer(InvocationOnMock invocationOnMock) throws Throwable {
          SaveImageCmd saveCmd = Mockito.mock(SaveImageCmd.class);
          Mockito.when(saveCmd.exec()).thenReturn(Mockito.mock(InputStream.class));
          return saveCmd;
        }
      }
    );

    return client;
  }
}
