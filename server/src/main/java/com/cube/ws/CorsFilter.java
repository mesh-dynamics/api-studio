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
                    // This is not a critical error. The browser/client may just have set a wrong string as Origin. We
                    // can safely ignore it and prevent access in this case, which will give the indication to client
                    // that something in their request is wrong.
                    //e.printStackTrace();
                    return Optional.empty();
                }
            });
        origin.ifPresent(originVal -> {
            if (originVal.getHost().equals("localhost")) {
                // only allowing from localhost for dev purposes
                // Browsers will block cross-origin access unless the server sets these fields explicitly and
                // specifies which domains to allow
                // Picked and modified from https://www.baeldung.com/cors-in-jax-rs
                responseContext.getHeaders().add(
                    "Access-Control-Allow-Origin", originVal.toString());
                responseContext.getHeaders().add(
                    "Access-Control-Allow-Credentials", "true");
                Optional.ofNullable(requestContext.getHeaderString("Access-Control-Request-Headers"))
                    .ifPresent(headers -> responseContext.getHeaders().add(
                        "Access-Control-Allow-Headers", headers));
                responseContext.getHeaders().add(
                    "Access-Control-Allow-Methods",
                    "GET, POST, PUT, DELETE, OPTIONS, HEAD");
            }
        });

    }
}
