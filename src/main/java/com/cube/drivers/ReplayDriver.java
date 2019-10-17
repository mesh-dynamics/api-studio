/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.drivers;

import com.cube.dao.Replay.ReplayStatus;
import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.cube.ws.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cube.agent.UtilException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.uri.UriComponent;
import org.json.JSONObject;

import com.cube.cache.ReplayResultCache;
import com.cube.core.Utils;
import com.cube.dao.Replay;
import com.cube.dao.ReqRespStore;
import com.cube.dao.Request;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-08
 * @author Prasad M D
 */
public class ReplayDriver  {

    private static final Logger LOGGER = LogManager.getLogger(ReplayDriver.class);

    private final Replay replay;
    public final ReqRespStore rrstore;
    private ReplayResultCache replayResultCache;
    private ObjectMapper jsonMapper;

    /**
     * @param endpoint
     * @param customerId
     * @param app
     * @param instanceId
     * @param collection
     * @param reqIds
     * @param replayId
     * @param async
     * @param status
     * @param sampleRate
     * @param templateVersion
     */
    private ReplayDriver(String endpoint, String customerId, String app, String instanceId, String collection, List<String> reqIds,
                         String replayId, boolean async, Replay.ReplayStatus status,
                         List<String> paths, int reqcnt, int reqsent, int reqfailed, String creationTimestamp,
                         Optional<Double> sampleRate, List<String> intermediateServices, Optional<String> templateVersion, Config config) {
        this(new Replay(endpoint, customerId, app, instanceId, collection, reqIds, replayId, async,
            templateVersion, status, paths, reqcnt, reqsent, reqfailed, creationTimestamp, sampleRate, intermediateServices), config);
    }

    /**
     * @param endpoint
     * @param customerId
     * @param app
     * @param collection
     * @param reqIds
     * @param replayId
     * @param async
     * @param status
     * @param sampleRate
     * @param templateVersion
     * @param config
     */
    private ReplayDriver(String endpoint, String customerId, String app, String instanceId,
        String collection, List<String> reqIds,
        String replayId, boolean async, ReplayStatus status,
        List<String> paths, Optional<Double> sampleRate, List<String> intermediateServices,
        Optional<String> templateVersion, Config config) {
        this(endpoint, customerId, app, instanceId, collection, reqIds, replayId, async,
            status, paths, 0, 0, 0, null, sampleRate, intermediateServices, templateVersion, config);
    }

    private ReplayDriver(Replay replay, Config config) {
        super();
        this.replay = replay;
        this.rrstore = config.rrstore;
        this.replayResultCache = config.replayResultCache;
        this.jsonMapper = config.jsonMapper;
    }


    private void replay() {

        //List<Request> requests = getRequests();

        if (replay.status != Replay.ReplayStatus.Init) {
            return;
        }
        replay.status = Replay.ReplayStatus.Running;
        if (!rrstore.saveReplay(replay))
            return;
        // start recording stats for the current replay
        //replayResultCache.startReplay(replay.customerId, replay.app, replay.instanceId, replay.replayId);

        // using seed generated from replayId so that same requests get picked in replay and analyze
        long seed = replay.replayId.hashCode();
        Random random = new Random(seed);

        // TODO: add support for matrix params

 	    /*
	    HttpClient client = HttpClient.newBuilder()
		        .version(Version.HTTP_1_1)
		        .followRedirects(Redirect.NORMAL)
		        .connectTimeout(Duration.ofSeconds(20))
		        .proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 80)))
		        .authenticator(Authenticator.getDefault())
		        .build();
		*/
        HttpClient.Builder clientbuilder = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1) // need to explicitly set this
            // if server is not supporting HTTP 2.0, getting a 403 error
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20));
        if (Authenticator.getDefault() != null)
            clientbuilder.authenticator(Authenticator.getDefault());
        HttpClient client = clientbuilder.build();

        Pair<Stream<List<Request>>, Long> batchedResult = replay.getRequestBatchesUsingEvents(BATCHSIZE, rrstore, jsonMapper);
        replay.reqcnt = batchedResult.getRight().intValue(); // NOTE: converting long to int, should be ok, since we
        // never replay so many requests

        batchedResult.getLeft().forEach(requests -> {

            // replay.reqcnt += requests.size();

            List<HttpRequest> reqs = new ArrayList<>();
            requests.forEach(r -> {
                /*
                 TODO: currently sampling samples across all paths with same rate. If we want to ensure that we have some
                 minimum requests from each path (particularly the rare ones), we need to add more logic
                */
                if (replay.sampleRate.map(sr -> random.nextDouble() > sr).orElse(false)) {
                    return; // drop this request
                }
                // transform fields in the request before the replay.
                replay.xfmer.ifPresent(x -> x.transformRequest(r));

                try {
                    UriBuilder uribuilder = UriBuilder.fromUri(replay.endpoint)
                        .path(r.apiPath);
                    r.queryParams.forEach(UtilException.rethrowBiConsumer((k, vlist) -> {
                        String[] params = vlist.stream().map(UtilException.rethrowFunction(v -> {
                            return UriComponent.encode(v, UriComponent.Type.QUERY_PARAM_SPACE_ENCODED);
                            // return URLEncoder.encode(v, "UTF-8"); // this had a problem of encoding space as +, which further gets encoded as %2B
                        })).toArray(String[]::new);
                        uribuilder.queryParam(k, (Object[])params);
                    }));
                    URI uri = uribuilder.build();
                    HttpRequest.Builder reqbuilder = HttpRequest.newBuilder()
                        .uri(uri)
                        .method(r.method, HttpRequest.BodyPublishers.ofString(r.body));

                    r.hdrs.forEach((k, vlist) -> {
                        // some headers are restricted and cannot be set on the request
                        // lua adds ':' to some headers which we filter as they are invalid
                        // and not needed for our requests.
                        if (Utils.ALLOWED_HEADERS.test(k) && !k.startsWith(":")) {
                            vlist.forEach(value -> reqbuilder.header(k, value));
                        }
                    });
                    // TODO: we can pass replayId to cubestore but currently requests don't match in the mock
                    // since we don't have the ability to ignore certain fields (in header and body)
                    // add the replayId so we can grab it while storing replayed requests & responses
                    // reqbuilder.header(Constants.CUBE_REPLAYID_HDRNAME, this.replayId);

                    reqs.add(reqbuilder.build());
                } catch (Exception e) {
                    // encode can throw UnsupportedEncodingException
                    LOGGER.error("Skipping request. Exception in creating uri: " + r.queryParams.toString(), e);
                }
            });


            List<Integer> respcodes = replay.async ? sendReqAsync(reqs.stream(), client) : sendReqSync(reqs.stream(), client);

            // count number of errors
            replay.reqfailed += respcodes.stream().filter(s -> (s != Response.Status.OK.getStatusCode())).count();
        });

        LOGGER.info(String.format("Replayed %d requests, got %d errors", replay.reqcnt, replay.reqfailed));

        replay.status = (replay.reqfailed == 0) ? Replay.ReplayStatus.Completed : Replay.ReplayStatus.Error;

        rrstore.saveReplay(replay);
        // stop recording stats for the current replay
        //replayResultCache.stopReplay(replay.customerId, replay.app , replay.instanceId, replay.replayId);
    }

    public boolean start() {

        if (replay.status != Replay.ReplayStatus.Init) {
            String message = String.format("Replay with id %s is already running or completed", replay.replayId);
            LOGGER.error(message);
            return false;
        }
        LOGGER.info(String.format("Starting replay with id %s", replay.replayId));
        CompletableFuture.runAsync(this::replay).handle((ret, e) -> {
            if (e != null) {
                LOGGER.error("Exception in replaying requests", e);
            }
            return ret;
        });
        return true;
    }

    public static Optional<Replay> getStatus(String replayId, ReqRespStore rrstore) {
        return rrstore.getReplay(replayId);
    }

    public static Optional<ReplayDriver> getReplayDriver(String replayId, Config config) {
        return getStatus(replayId, config.rrstore).map(r -> new ReplayDriver(r, config));
    }


    public static Optional<ReplayDriver> initReplay(String endpoint, String customerId, String app, String instanceId,
                                              String collection, List<String> reqIds, boolean async, List<String> paths,
                                              JSONObject xfms, Optional<Double> sampleRate, List<String> intermediateServices, Optional<String> templateSetVersion, Config config) {
        String replayId = Replay.getReplayIdFromCollection(collection);
        ReplayDriver replaydriver = new ReplayDriver(endpoint, customerId, app, instanceId, collection,
                reqIds, replayId, async, Replay.ReplayStatus.Init, paths, sampleRate, intermediateServices, templateSetVersion, config);
        if (config.rrstore.saveReplay(replaydriver.replay)) {
            return Optional.of(replaydriver);
        }
        return Optional.empty();
    }

    public Replay getReplay() {
        return replay;
    }

    private static int UPDBATCHSIZE = 10; // replay metadata will be updated after each such batch
    private static int BATCHSIZE = 40; // this controls the number of requests in a batch that could be sent in async fashion

    private List<Integer> sendReqAsync(Stream<HttpRequest> httprequests, HttpClient client) {
        // exceptions are converted to status code indicating error
        List<CompletableFuture<Integer>> respcodes = httprequests.map(request -> {
            replay.reqsent++;
            if (replay.reqsent % UPDBATCHSIZE == 0) {
                LOGGER.info(String.format("Replay %s sent %d requests", replay.replayId, replay.reqsent));
                rrstore.saveReplay(replay);
            }
            return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(HttpResponse::statusCode).handle((ret, e) -> {
                    if (e != null) {
                        LOGGER.error("Exception in replaying requests", e);
                        return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
                    }
                    return ret;
                });
        }).collect(Collectors.toList());
        CompletableFuture<List<Integer>> rcodes = Utils.sequence(respcodes);

        try {
            return rcodes.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Exception in replaying requests", e);
            return Collections.nCopies(respcodes.size(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    private List<Integer> sendReqSync(Stream<HttpRequest> httprequests, HttpClient client) {

        List<Integer> respcodes = httprequests.map(request -> {
            try {
                replay.reqsent++;
                if (replay.reqsent % UPDBATCHSIZE == 0) {
                    LOGGER.info(String.format("Replay %s completed %d requests", replay.replayId, replay.reqsent));
                    rrstore.saveReplay(replay);
                }
                int ret = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                // for debugging - can remove later
                if (ret != Response.Status.OK.getStatusCode()) {
                    LOGGER.error(String.format("Got error status %d for req: %s", ret, request.toString()));
                }
                return ret;
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Exception in replaying requests", e);
                return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            }
        }).collect(Collectors.toList());
        return respcodes;
    }

}
