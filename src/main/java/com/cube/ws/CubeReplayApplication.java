package com.cube.ws;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/")
public class CubeReplayApplication extends ResourceConfig {
    public CubeReplayApplication() {
        // where the Config class is
        packages("com.cube.ws");
        register(new Binder());
    }
}
