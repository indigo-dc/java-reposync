package com.atos.indigo.reposync;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

/**
 * Created by jose on 26/05/16.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ConfigurationManager.class)
public class AuthorizationFilterTest {

  AuthorizationRequestFilter filter = new AuthorizationRequestFilter();

  @Test
  public void testAuthorization() throws IOException {

    PowerMockito.mockStatic(ConfigurationManager.class);
    Mockito.when(ConfigurationManager.getProperty(ReposyncTags.REPOSYNC_TOKEN)).thenReturn("test");

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
