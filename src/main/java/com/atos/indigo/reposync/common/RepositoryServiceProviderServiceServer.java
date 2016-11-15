package com.atos.indigo.reposync.common;

import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.glassfish.jersey.server.ChunkedOutput;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Created by jose on 11/15/16.
 */
@Path("v1.0")
public interface RepositoryServiceProviderServiceServer extends RepositoryServiceProviderService {

  @PUT
  @Path("images")
  @Produces(MediaType.APPLICATION_JSON)
  ChunkedOutput<ImageInfoBean> pull(
      @QueryParam("imageName") String imageName,
      @QueryParam("tag") String tag);

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("notify")
  ChunkedOutput<ImageInfoBean> notify(@QueryParam("token") String token,
                                      ObjectNode payload);

  @PUT
  @Path("sync")
  @Produces(MediaType.APPLICATION_JSON)
  ChunkedOutput<String> sync();

}
