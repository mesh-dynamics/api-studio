/**
 * Copyright Cube I O
 */
package com.cube.drivers;

import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStore.RR;
import com.cube.dao.ReqRespStore.Request;

/**
 * @author prasad
 *
 */
public class Replay {

    private static final Logger LOGGER = LogManager.getLogger(Replay.class);

	public enum ReplayStatus {
		Init,
		Running,
		Completed,
		Error
	}
	
	
	
	/**
	 * @param endpoint
	 * @param customerid
	 * @param app
	 * @param collection
	 * @param reqids
	 * @param rrstore
	 * @param replayid
	 * @param async
	 * @param status 
	 */
	public Replay(String endpoint, String customerid, String app, String collection, List<String> reqids,
			ReqRespStore rrstore,  String replayid, boolean async, ReplayStatus status) {
		super();
		this.endpoint = endpoint;
		this.customerid = customerid;
		this.app = app;
		this.collection = collection;
		this.reqids = reqids;
		this.rrstore = rrstore;
		this.replayid = replayid;
		this.async = async;
		this.status = status;
	}

	private void replay() {
		
		List<Request> requests = rrstore.getRequests(customerid, app, collection, reqids, RR.Record);

		status = ReplayStatus.Running;
		if (!rrstore.saveReplay(this))
			return;

		
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
	    HttpClient client = HttpClient.newBuilder()
		        .followRedirects(Redirect.NORMAL)
		        .connectTimeout(Duration.ofSeconds(20))
		        .authenticator(Authenticator.getDefault())
		        .build();
	   
	    int errcnt = 0;
		Stream<HttpRequest> httprequests = requests.stream().map(r -> {
			UriBuilder uribuilder = UriBuilder.fromUri(endpoint)
					.path(r.path);
			r.qparams.forEach((k, vlist) -> {
				uribuilder.queryParam(k, vlist.toArray());
			});
			URI uri = uribuilder.build();
			HttpRequest.Builder reqbuilder = HttpRequest.newBuilder()
					.uri(uri)
					.method(r.method, BodyPublishers.ofString(r.body));

			r.hdrs.forEach((k, vlist) -> {
				vlist.forEach(value -> {
					reqbuilder.header(k, value);					
				});
			});
						
			return reqbuilder.build();
		});
		
		List<Integer> respcodes = async ? sendReqAsync(httprequests, client) : sendReqSync(httprequests, client);

		// count number of errors
		errcnt = respcodes.stream().map(s -> {
			if (s != Response.Status.OK.getStatusCode())
				return 1;
			else 
				return 0;
		}).reduce(Integer::sum).orElse(0);
		
		LOGGER.info(String.format("Replayed %d requests, got %d errors", requests.size(), errcnt));

		status = (errcnt == 0) ? ReplayStatus.Completed : ReplayStatus.Error;
		
		rrstore.saveReplay(this);
		
		return;
		
	}
	
	public void start() {

		LOGGER.info(String.format("Starting replay with id %d", replayid));
		CompletableFuture.runAsync(() -> replay());		
	}
	
	public static Optional<ReplayStatus> getStatus(String replayid, ReqRespStore rrstore) {
		return rrstore.getReplay(replayid).map(r -> r.status);
	}

	public static Optional<Replay> initReplay(String endpoint, String customerid, String app, String collection, List<String> reqids,
			ReqRespStore rrstore, boolean async) {
		String replayid = String.format("%s-%s", collection, UUID.randomUUID().toString());
		Replay replay = new Replay(endpoint, customerid, app, collection, reqids, rrstore, replayid, async, ReplayStatus.Init);
	
		if (rrstore.saveReplay(replay))
			return Optional.of(replay);
		return Optional.empty();
	}
	
	private List<Integer> sendReqAsync(Stream<HttpRequest> httprequests, HttpClient client) {
		// exceptions are converted to status code indicating error
		List<CompletableFuture<Integer>> respcodes = httprequests.map(request -> {
			return client.sendAsync(request, BodyHandlers.discarding())
					.thenApply(HttpResponse::statusCode).handle((ret, e) -> {
						LOGGER.error("Exception in replaying requests", e);
						return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
					});
		}).collect(Collectors.toList());
		CompletableFuture<List<Integer>> rcodes = sequence(respcodes);
		
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
				return client.send(request, BodyHandlers.discarding()).statusCode();
			} catch (IOException | InterruptedException e) {
				LOGGER.error("Exception in replaying requests", e);
				return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
			}
		}).collect(Collectors.toList());
		return respcodes;
	}

	
	private static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
	    CompletableFuture<Void> allDoneFuture =
	        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
	    return allDoneFuture.thenApply(v ->
	            futures.stream().
	                    map(future -> future.join()).
	                    collect(Collectors.<T>toList())
	    );
	}
	
	
	
	public final String endpoint;
	public final String customerid;
	public final String app;
	public final String collection;
	public final List<String> reqids;
	public final ReqRespStore rrstore;
	public final String replayid; // this needs to be globally unique
	public final boolean async;
	public ReplayStatus status;
}
