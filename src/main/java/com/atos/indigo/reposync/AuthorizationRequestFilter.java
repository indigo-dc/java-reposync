package com.atos.indigo.reposync;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Created by jose on 11/05/16.
 */
@Authorized
public class AuthorizationRequestFilter implements ContainerRequestFilter{
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String token = requestContext.getHeaderString(ReposyncTags.TOKEN_HEADER);
        if (token == null || !token.equals(System.getProperty(ReposyncTags.REPOSYNC_TOKEN))) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Authorization token needed").build());
        }
    }
    }
