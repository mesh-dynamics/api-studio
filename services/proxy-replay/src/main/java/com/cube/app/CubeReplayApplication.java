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

package com.cube.app;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

public class CubeReplayApplication {
    private static final Logger LOGGER = LogManager.getLogger(CubeReplayApplication.class);

    public static void main(String[] args) throws Exception {

        String portValue = "9992";

        if (args.length > 0) {
            portValue = args[0];
        }
        int port = Integer.valueOf(portValue);

        Server jettyServer = new Server(port);
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setContextPath("/");
        jettyServer.setHandler(ctx);
        ServletHolder servlet = ctx.addServlet(ServletContainer.class, "/*");
        servlet.setInitOrder(1);
        servlet.setInitParameter(ServerProperties.PROVIDER_PACKAGES, "com.cube.ws");

        try {
            jettyServer.start();
            jettyServer.join();
        } catch (Exception ex) {
            LOGGER.error("Error while starting the jetty", ex.getMessage());
        }
    }

    }
