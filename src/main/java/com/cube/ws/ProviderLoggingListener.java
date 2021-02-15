package com.cube.ws;

import java.util.Set;

import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;


/*
 * Created by IntelliJ IDEA.
 * Date: 13/02/2021
 * This is just for debugging purposes to see all the resources configured
 * https://stackoverflow.com/questions/37084029/how-to-list-all-registered-jax-rs-entity-providers-in-jersey
 */
@Provider
public class ProviderLoggingListener implements ApplicationEventListener {

    @Override
    public void onEvent(ApplicationEvent event) {
        switch (event.getType()) {
            case INITIALIZATION_FINISHED: {
                Set<Class<?>> providers = event.getProviders();
                ResourceConfig immutableConfig = event.getResourceConfig();
                ResourceModel resourcesModel = event.getResourceModel();
                break;
            }
        }
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return null;
    }
}
