/**
 * Copyright Cube I O
 */
package com.cube.ws;

import static io.cube.agent.Constants.AUTHORIZATION_HEADER;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.ProxyAnalyzer;
import io.cube.agent.ProxyDataStore;
import io.md.constants.ReplayStatus;
import io.md.core.Utils;
import io.md.dao.Recording;
import io.md.dao.Replay;
import io.md.services.Analyzer;
import io.md.utils.Constants;
import io.md.ws.ReplayBasicWS;

/**
 * The proxy replay service
 */
@Path("/rs")
public class ReplayWS extends ReplayBasicWS {

    private static final Logger LOGGER = LogManager.getLogger(ReplayWS.class);


    @Path("/health")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        Map respMap = new HashMap();
        respMap.put(Constants.SERVICE_HEALTH_STATUS, "RS is healthy");
        return Response.ok().type(MediaType.APPLICATION_JSON).entity((new JSONObject(respMap)).toString()).build();
    }


    @Override
    protected CompletableFuture<Void> afterReplay(HttpHeaders headers, MultivaluedMap<String, String> formParams,
                                                  Recording recording, Replay replay, Optional<Analyzer> analyzerOpt) {

        return CompletableFuture.runAsync(() -> analyze(replay, analyzerOpt));
    }

    @Override
    protected void beforeApi(HttpHeaders headers) {
        Utils.getFirst(headers.getRequestHeaders(), AUTHORIZATION_HEADER)
            .ifPresent(authToken -> {
                ((ProxyDataStore) super.dataStore).setAuthToken(authToken);
                ((ProxyAnalyzer) super.analyzer).setAuthToken(authToken);
            });
        super.beforeApi(headers);
    }

    /**
     */
	@Inject
	public ReplayWS() {
	    super(new ProxyDataStore(), new ProxyAnalyzer());
    }


    ObjectMapper jsonMapper;
}
