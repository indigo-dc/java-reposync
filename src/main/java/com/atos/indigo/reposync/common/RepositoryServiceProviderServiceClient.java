package com.atos.indigo.reposync.common;

import com.atos.indigo.reposync.beans.ImageInfoBean;

import org.glassfish.jersey.client.ChunkedInput;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Created by jose on 11/15/16.
 */
@Path("v1.0")
public interface RepositoryServiceProviderServiceClient extends RepositoryServiceProviderService {

  @PUT
  @Path("images")
  @Produces(MediaType.APPLICATION_JSON)
  ChunkedInput<ImageInfoBean> pull(
      @QueryParam("imageName") String imageName,
      @QueryParam("tag") String tag,
      String body);

  @PUT
  @Path("sync")
  @Produces(MediaType.APPLICATION_JSON)
  ChunkedInput<String> sync(String body);

}
