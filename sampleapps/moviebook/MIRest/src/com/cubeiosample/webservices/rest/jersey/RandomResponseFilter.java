package com.cubeiosample.webservices.rest.jersey;

import org.apache.log4j.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Random;
import java.util.Date;

@Provider
public class RandomResponseFilter implements ContainerRequestFilter {

    final static Logger LOGGER = Logger.getLogger(RandomResponseFilter.class);
    private final Random random = new Random();
    private static Config config = new Config();
    private long requestTimeStamp = new Date().getTime();
    private Double randomGuassianPercentGivenStdDevAndMean = random.nextGaussian() * config.FAIL_PERCENT_STD_DEV + config.FAIL_PERCENT;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        /*
            This filter fails requests randomly.
            Changing the random fail percent between runs.
            Ideally it should be updated with an API hook when a new replay starts.
            For now it is updated every 60 seconds assuming we dont run replays too often
         */
        long currentRequestTimeStamp = new Date().getTime();
        if (requestTimeStamp + config.TIME_BETWEEN_RUNS > currentRequestTimeStamp) {
            LOGGER.debug("Random fail percent updated");
            randomGuassianPercentGivenStdDevAndMean = random.nextGaussian() * config.FAIL_PERCENT_STD_DEV + config.FAIL_PERCENT;
        }
        requestTimeStamp = currentRequestTimeStamp;

        if (random.nextDouble() < randomGuassianPercentGivenStdDevAndMean) {
            LOGGER.debug("Forcing the request to abort");
            containerRequestContext.abortWith(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .header(HttpHeaders.RETRY_AFTER, " :=120")
                            .build());
        }
    }
}
