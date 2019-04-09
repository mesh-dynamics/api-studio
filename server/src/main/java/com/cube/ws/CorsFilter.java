/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.ws;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-04-09
 * @author Prasad M D
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        // TODO: revisit this later to allow request from only some origins

        Optional<URL> origin = Optional.ofNullable(requestContext.getHeaderString("Origin"))
            .flatMap(url -> {
                try {
                    return Optional.of(new URL(url));
                } catch (MalformedURLException e) {
                    //e.printStackTrace();
                    return Optional.empty();
                }
            });
        origin.ifPresent(originVal -> {
            if (originVal.getHost().equals("localhost")) {
                // only allowing from localhost for dev purposes
                responseContext.getHeaders().add(
                    "Access-Control-Allow-Origin", originVal.toString());
                responseContext.getHeaders().add(
                    "Access-Control-Allow-Credentials", "true");
                responseContext.getHeaders().add(
                    "Access-Control-Allow-Headers",
                    "origin, content-type, accept, authorization");
                responseContext.getHeaders().add(
                    "Access-Control-Allow-Methods",
                    "GET, POST, PUT, DELETE, OPTIONS, HEAD");
            }
        });

    }
}
