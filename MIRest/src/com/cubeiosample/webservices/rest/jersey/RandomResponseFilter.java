package com.cubeiosample.webservices.rest.jersey;

import org.apache.log4j.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Random;

@Provider
public class RandomResponseFilter implements ContainerRequestFilter {

    final static Logger LOGGER = Logger.getLogger(RandomResponseFilter.class);
    private final Random random = new Random();
    private static Config config = new Config();

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        Double randomGuassianPercentGivenStdDevAndMean = random.nextGaussian() * config.FAIL_PERCENT_STD_DEV + config.FAIL_PERCENT;
        try {
            if (random.nextDouble() < randomGuassianPercentGivenStdDevAndMean) {
                containerRequestContext.wait(1000000);
                containerRequestContext.abortWith(
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .header(HttpHeaders.RETRY_AFTER, " :=120")
                                .build());
            }
        } catch (InterruptedException ex) {
            LOGGER.error(ex.getMessage());
        }

    }
}
