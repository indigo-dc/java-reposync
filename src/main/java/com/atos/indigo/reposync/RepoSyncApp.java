package com.atos.indigo.reposync;

import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

/**
 * Created by jose on 19/05/16.
 */
public class RepoSyncApp extends Application {

    //Add Service APIs
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<Class<?>>();

        //register REST modules
        resources.add(RepositoryServiceProviderService.class);

        //Manually adding JacksonJSONFeature
        resources.add(JacksonFeatures.class);
        resources.add(JacksonJsonProvider.class);

        return resources;
    }
}
