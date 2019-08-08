package com.cube.ws;

import java.io.IOException;
import java.util.Optional;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import io.cube.agent.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.opentracing.Scope;

import com.cube.core.Utils;

@Provider
public class TracingFilter implements ContainerRequestFilter , ContainerResponseFilter {

    private static final Logger LOGGER = LogManager.getLogger(TracingFilter.class);
    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        LOGGER.debug("Inside Method :: " + resourceInfo.getResourceMethod().getName() + " "
            + resourceInfo.getResourceClass().getName());
        Scope scope = CommonUtils.startServerSpan(containerRequestContext.getHeaders() ,
            resourceInfo.getResourceClass().getSimpleName() + "-" + resourceInfo.getResourceMethod().getName());
        containerRequestContext.setProperty("scope" , scope);
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        Scope scope = (Scope) containerRequestContext.getProperty("scope");
        if (scope != null) {
            LOGGER.debug("Closing scope");
            scope.close();
        }
    }
}
