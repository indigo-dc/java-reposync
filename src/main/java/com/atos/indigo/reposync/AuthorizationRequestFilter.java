package com.atos.indigo.reposync;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Created by jose on 11/05/16.
 */
public class AuthorizationRequestFilter implements ContainerRequestFilter{
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String token = requestContext.getHeaderString(RepositoryServiceProviderService.TOKEN);
        if (token == null || !token.equals(System.getProperty(RepositoryServiceProviderService.TOKEN))) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Authorization token needed").build());
        }
    }
    }
