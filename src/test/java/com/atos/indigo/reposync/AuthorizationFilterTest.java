package com.atos.indigo.reposync;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

/**
 * Created by jose on 26/05/16.
 */
public class AuthorizationFilterTest {

  AuthorizationRequestFilter filter = new AuthorizationRequestFilter();

  @Test
  public void testAuthorization() throws IOException {

    System.setProperty(ReposyncTags.REPOSYNC_TOKEN, "test");

    // Right token
    ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);

    Mockito.when(context.getHeaderString(ReposyncTags.TOKEN_HEADER)).thenReturn("test");

    filter.filter(context);

    Mockito.verify(context,Mockito.times(0)).abortWith(Matchers.any(Response.class));

    // Wrong token
    Mockito.when(context.getHeaderString(ReposyncTags.TOKEN_HEADER)).thenReturn("falseToken");

    filter.filter(context);

    Mockito.verify(context,Mockito.times(1)).abortWith(Matchers.any(Response.class));

    // Unexisting token
    Mockito.when(context.getHeaderString(ReposyncTags.TOKEN_HEADER)).thenReturn(null);

    filter.filter(context);

    Mockito.verify(context,Mockito.times(2)).abortWith(Matchers.any(Response.class));


  }

}
