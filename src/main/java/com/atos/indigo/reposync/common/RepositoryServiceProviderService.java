package com.atos.indigo.reposync.common;

import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Created by jose on 11/15/16.
 */
public interface RepositoryServiceProviderService {

  @GET
  @Path("images")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  List<ImageInfoBean> images(@QueryParam("filter") String filter);


  @DELETE
  @Path("images/{imageId}")
  @Produces(MediaType.APPLICATION_JSON)
  ActionResponseBean delete(@PathParam("imageId") String imageId);

}
