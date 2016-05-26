package com.atos.indigo.reposync;

import com.atos.indigo.reposync.providers.OpenNebulaRepositoryServiceProvider;
import com.github.dockerjava.api.DockerClient;

import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.datastore.Datastore;
import org.opennebula.client.datastore.DatastorePool;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Created by jose on 25/05/16.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Image.class)
public class OpenNebulaProviderTest extends RepositoryServiceProviderTest<Image> {

  private int idCounter = 6;

  protected Image mockImage(final String id, String name, String dockerId,
                          String dockerName, String dockerTag) {

    final Image result = Mockito.mock(Image.class);
    Mockito.when(result.getId()).thenReturn(id);
    Mockito.when(result.getName()).thenReturn(name);
    Mockito.when(result.xpath(
      OpenNebulaRepositoryServiceProvider.TEMPLATE_BASE+
        OpenNebulaRepositoryServiceProvider.DOCKER_ID)).thenReturn(dockerId);
    Mockito.when(result.xpath(
      OpenNebulaRepositoryServiceProvider.TEMPLATE_BASE+
        OpenNebulaRepositoryServiceProvider.DOCKER_NAME)).thenReturn(dockerName);
    Mockito.when(result.xpath(
      OpenNebulaRepositoryServiceProvider.TEMPLATE_BASE+
        OpenNebulaRepositoryServiceProvider.DOCKER_TAG)).thenReturn(dockerTag);

    Mockito.when(result.delete()).thenAnswer(new Answer<OneResponse>() {
      @Override
      public OneResponse answer(InvocationOnMock invocationOnMock) throws Throwable {
        Image img = (Image) invocationOnMock.getMock();
        OneResponse result = Mockito.mock(OneResponse.class);
        if (imageList.remove(img)) {
          Mockito.when(result.isError()).thenReturn(false);
        } else {
          Mockito.when(result.isError()).thenReturn(true);
          Mockito.when(result.getErrorMessage()).thenReturn("Image "+id+" not found");
        }
        return result;
      }
    });

    return result;
  }

  private OneResponse mockResponse(boolean isError) {
    OneResponse response = Mockito.mock(OneResponse.class);
    Mockito.when(response.isError()).thenReturn(isError);
    return response;
  }

  private Datastore mockDatastore() {
    Datastore dockStore = Mockito.mock(Datastore.class);
    Mockito.when(dockStore.getName())
      .thenReturn(OpenNebulaRepositoryServiceProvider.ONEDOCK_DATASTORE_NAME);
    Mockito.when(dockStore.id()).thenReturn(1);
    return dockStore;
  }

  public void setUp() {
    Client client = Mockito.mock(Client.class);
    ImagePool imgPool = Mockito.mock(ImagePool.class);

    PowerMockito.mockStatic(Image.class);

    Mockito.when(Image.allocate(Matchers.any(Client.class),Matchers.anyString(),Matchers.anyInt()))
      .thenAnswer(new Answer<OneResponse>() {
        @Override
        public OneResponse answer(InvocationOnMock invocationOnMock) throws Throwable {
          String template = (String) invocationOnMock.getArguments()[1];
          Properties props = new Properties();
          props.load(new StringReader(template));

          Image newImage = mockImage(
            Integer.toString(idCounter),
            props.getProperty("NAME"),
            props.getProperty(OpenNebulaRepositoryServiceProvider.DOCKER_ID),
            props.getProperty(OpenNebulaRepositoryServiceProvider.DOCKER_NAME),
            props.getProperty(OpenNebulaRepositoryServiceProvider.DOCKER_TAG));

          imageList.add(newImage);

          OneResponse response = mockResponse(false);
          Mockito.when(response.getMessage()).thenReturn(Integer.toString(idCounter));
          idCounter++;

          return response;
        }
      });

    OneResponse success = mockResponse(false);
    Mockito.when(imgPool.info()).thenReturn(success);
    Mockito.when(imgPool.infoAll()).thenReturn(success);

    Mockito.when(imgPool.iterator()).thenAnswer(new Answer<Iterator<Image>>() {
      @Override
      public Iterator<Image> answer(InvocationOnMock invocationOnMock) throws Throwable {
        return imageList.iterator();
      }
    });

    Mockito.when(imgPool.getById(Matchers.anyInt())).thenAnswer(new Answer<Image>() {
      @Override
      public Image answer(InvocationOnMock invocationOnMock) throws Throwable {
        Integer id = (Integer)invocationOnMock.getArguments()[0];
        for (Image img : imageList) {
          if (id.equals(new Integer(img.getId()))) {
            return img;
          }
        }

        return null;
      }
    });

    DatastorePool dataPool = Mockito.mock(DatastorePool.class);
    Mockito.when(dataPool.info()).thenReturn(success);

    final List<Datastore> dataStores = new ArrayList<>();
    dataStores.add(mockDatastore());
    Mockito.when(dataPool.iterator()).thenAnswer(new Answer<Iterator<Datastore>>() {
      @Override
      public Iterator<Datastore> answer(InvocationOnMock invocationOnMock) throws Throwable {
        return dataStores.iterator();
      }
    });

    provider = new OpenNebulaRepositoryServiceProvider(client,imgPool,dataPool);

  }

  @Override
  protected DockerClient mockDockerClient() {
    return Mockito.mock(DockerClient.class);
  }

}
