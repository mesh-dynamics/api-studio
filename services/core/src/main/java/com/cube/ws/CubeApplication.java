package com.cube.ws;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import io.md.cube.jaxrs.ingress.LoggingFilter;
import io.md.cube.jaxrs.ingress.TracingFilter;

@ApplicationPath("/")
public class CubeApplication extends ResourceConfig {
    public CubeApplication() {
        // where the Config class is
        packages("com.cube.ws");
        register(LoggingFilter.class);
        register(TracingFilter.class);
        register(new Binder());
        register(MultiPartFeature.class);
    }
}