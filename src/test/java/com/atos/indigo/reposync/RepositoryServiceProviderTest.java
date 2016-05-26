package com.atos.indigo.reposync;

import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.atos.indigo.reposync.providers.RepositoryServiceProvider;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jose on 26/05/16.
 */
public abstract class RepositoryServiceProviderTest<T> {

  protected RepositoryServiceProvider provider = null;
  protected List<T> imageList = new ArrayList<>();

  protected abstract T mockImage(final String id, String name, String dockerId,
                              String dockerName, String dockerTag);

  @Before
  public void setUpData(){

    imageList.add(mockImage("1","Ubuntu","docId1","ubuntu","latest"));
    imageList.add(mockImage("2","Kubuntu","docId2","kubuntu","14.04"));
    imageList.add(mockImage("3","Busybox","docId3","busybox","latest"));
    imageList.add(mockImage("4","Mysql_server","docId4","mysql/mysql-server","5.5"));
    imageList.add(mockImage("5","Red Hat Linux",null,null,null));

    setUp();

  }

  public abstract void setUp();

  protected abstract DockerClient mockDockerClient();

  @Test
  public void testList() {
    List<ImageInfoBean> allImgs = provider.images(null);

    assert(allImgs.size() == 5);
    for (ImageInfoBean img: allImgs) {
      assert(img.getId() != null);
      assert(img.getName() != null);
      assert(img.getType() != null);

      if ("5".equals(img.getId())) {
        assert(ImageInfoBean.ImageType.VM.equals(img.getType()));
        assert(img.getDockerId() == null);
        assert(img.getDockerName() == null);
        assert(img.getDockerTag() == null);
      } else {
        assert(ImageInfoBean.ImageType.DOCKER.equals(img.getType()));
        assert(img.getDockerId() != null);
        assert(img.getDockerName() != null);
        assert(img.getDockerTag() != null);
      }
    }

    List<ImageInfoBean> ubuntus = provider.images("*buntu");
    assert(ubuntus.size() == 2);

  }

  @Test
  public void testDelete() {
    ActionResponseBean response = provider.delete("1");
    assert(response.isSuccess() == true);
    assert(imageList.size() == 4);

    response = provider.delete("10");
    assert(response.isSuccess() == false);
    assert(response.getErrorMessage() != null);
    assert(imageList.size() == 4);
  }

  @Test
  public void testUpdate() {
    InspectImageResponse toUpdate = Mockito.mock(InspectImageResponse.class);
    Mockito.when(toUpdate.getId()).thenReturn("newDockId1");

    DockerClient dockerClient = mockDockerClient();

    ImageInfoBean newImg = provider.imageUpdated("ubuntu", "latest", toUpdate,
      dockerClient);

    assert(newImg.getType().equals(ImageInfoBean.ImageType.DOCKER));
    assert(newImg.getDockerId().equals("newDockId1"));
    assert(newImg.getName().equals("ubuntu"));
    assert(newImg.getDockerTag().equals("latest"));
    assert(imageList.size() == 5);


    Mockito.when(toUpdate.getId()).thenReturn("freshDockId1");
    newImg = provider.imageUpdated("opensuse", "42.1", toUpdate,
      dockerClient);

    assert(newImg.getType().equals(ImageInfoBean.ImageType.DOCKER));
    assert(newImg.getDockerId().equals("freshDockId1"));
    assert(newImg.getName().equals("opensuse"));
    assert(newImg.getDockerTag().equals("42.1"));
    assert(imageList.size() == 6);

  }

}
