/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
