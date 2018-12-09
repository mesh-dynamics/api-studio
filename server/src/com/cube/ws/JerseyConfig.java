/**
 * Copyright Cube I O
 */
package com.cube.ws;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * @author prasad
 *
 */
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        // where the Config class is
        packages("com.cube.ws"); 
        register(new Binder());
    }
}
