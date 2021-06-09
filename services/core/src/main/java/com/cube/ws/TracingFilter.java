package com.cube.ws;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;

@Provider
public class TracingFilter implements ContainerRequestFilter , ContainerResponseFilter {

    private static final Logger LOGGER = LogManager.getLogger(TracingFilter.class);
    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        // deliberately not generating span for health calls (to stop bombarding logs)
        if ("health".equals(resourceInfo.getResourceMethod().getName())) return;
        LOGGER.debug(new ObjectMessage(Map.of("method" ,resourceInfo.getResourceMethod()
            .getName(), "resourceClass" ,  resourceInfo.getResourceClass().getName())));
        Span span = CommonUtils.startServerSpan(containerRequestContext.getHeaders() ,
            resourceInfo.getResourceClass().getSimpleName() + "-"
                + resourceInfo.getResourceMethod().getName());
        Scope scope = CommonUtils.activateSpan(span);
        containerRequestContext.setProperty("scope" , scope);
        containerRequestContext.setProperty("span" , span);
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext
        , ContainerResponseContext containerResponseContext) throws IOException {
        Object scope = containerRequestContext.getProperty("scope");
        if (scope != null) {
            LOGGER.debug("Closing scope");
            ((Scope)scope).close();
        }
        Object span = containerRequestContext.getProperty("span");
        if (span != null) {
            LOGGER.debug("Closing span");
            ((Span)span).finish();
        }
    }
}
