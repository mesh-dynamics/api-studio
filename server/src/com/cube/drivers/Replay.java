/**
 * Copyright Cube I O
 */
package com.cube.drivers;

import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cube.core.Utils;
import com.cube.dao.RRBase;
import com.cube.dao.ReqRespStore;
import com.cube.dao.Request;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
			ReqRespStore rrstore,  String replayid, boolean async, ReplayStatus status,
			List<String> paths, int reqcnt, int reqsent, int reqfailed) {
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
		this.paths = paths;
		this.reqcnt = reqcnt;
		this.reqsent = reqsent;
		this.reqfailed = reqfailed;
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
	private Replay(String endpoint, String customerid, String app, String collection, 
			List<String> reqids,
			ReqRespStore rrstore,  String replayid, boolean async, ReplayStatus status,
			List<String> paths) {
		this(endpoint, customerid, app, collection, reqids, rrstore, replayid, async, 
				status, paths, 0, 0, 0);
	}
	

	private void replay() {
		
		List<Request> requests = getRequests();

		if (status != ReplayStatus.Init) {
			return;
		}
		status = ReplayStatus.Running;
		reqcnt = requests.size();
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
	    HttpClient.Builder clientbuilder = HttpClient.newBuilder()
	    		.version(Version.HTTP_1_1) // need to explicitly set this
	    		// if server is not supporting HTTP 2.0, getting a 403 error
		        .followRedirects(Redirect.NORMAL)
		        .connectTimeout(Duration.ofSeconds(20));
	    if (Authenticator.getDefault() != null)
	    	clientbuilder.authenticator(Authenticator.getDefault());
	    HttpClient client = clientbuilder.build();
	   
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
				// some headers are restricted and cannot be set on the request
				if (Utils.ALLOWED_HEADERS.test(k)) {
					vlist.forEach(value -> {
						reqbuilder.header(k, value);					
					});
				}
			});
						
			return reqbuilder.build();
		});
		
		List<Integer> respcodes = async ? sendReqAsync(httprequests, client) : sendReqSync(httprequests, client);

		// count number of errors
		reqfailed = respcodes.stream().map(s -> {
			if (s != Response.Status.OK.getStatusCode())
				return 1;
			else 
				return 0;
		}).reduce(Integer::sum).orElse(0);
		
		LOGGER.info(String.format("Replayed %d requests, got %d errors", requests.size(), reqfailed));

		status = (reqfailed == 0) ? ReplayStatus.Completed : ReplayStatus.Error;
		
		rrstore.saveReplay(this);
		
		return;
		
	}
	
	public boolean start() {

		if (status != ReplayStatus.Init) {
			String message = String.format("Replay with id %s is already running or completed", replayid);
			LOGGER.error(message);
			return false;
		}
		LOGGER.info(String.format("Starting replay with id %s", replayid));
		CompletableFuture.runAsync(() -> replay()).handle((ret, e) -> {
			if (e != null) {
				LOGGER.error("Exception in replaying requests", e);
			}
			return ret;
		});
		return true;
	}
	
	public static Optional<Replay> getStatus(String replayid, ReqRespStore rrstore) {
		return rrstore.getReplay(replayid);
	}

	public static Optional<Replay> initReplay(String endpoint, String customerid, String app, String collection, List<String> reqids,
			ReqRespStore rrstore, boolean async, List<String> paths) {
		String replayid = getReplayIdFromCollection(collection);
		Replay replay = new Replay(endpoint, customerid, app, collection, reqids, rrstore, replayid, async, ReplayStatus.Init, paths);
	
		if (rrstore.saveReplay(replay))
			return Optional.of(replay);
		return Optional.empty();
	}
	
	private static int UPDBATCHSIZE = 10;
	
	private List<Integer> sendReqAsync(Stream<HttpRequest> httprequests, HttpClient client) {
		// exceptions are converted to status code indicating error
		List<CompletableFuture<Integer>> respcodes = httprequests.map(request -> {
			reqsent++;
			if (reqsent % UPDBATCHSIZE == 0) {
				LOGGER.info(String.format("Replay %s sent %d requests", replayid, reqsent));
				rrstore.saveReplay(this);
			}
			return client.sendAsync(request, BodyHandlers.discarding())
					.thenApply(HttpResponse::statusCode).handle((ret, e) -> {
						if (e != null) {
							LOGGER.error("Exception in replaying requests", e);
							return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
						}
						return ret;
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
				reqsent++;
				if (reqsent % UPDBATCHSIZE == 0) {
					LOGGER.info(String.format("Replay %s completed %d requests", replayid, reqsent));
					rrstore.saveReplay(this);
				}
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
	@JsonIgnore
	public final ReqRespStore rrstore;
	public final String replayid; // this needs to be globally unique
	public final boolean async;
	public ReplayStatus status;
	public final List<String> paths; // paths to be replayed
	public int reqcnt; // total number of requests
	public int reqsent; // number of requests sent
	public int reqfailed; // requests failed, return code not 200

	static final String uuidpatternStr = "\\b[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-\\b[0-9a-fA-F]{12}\\b";
	static final String replayidpatternStr = "^(.*)-" + uuidpatternStr + "$";
	private static final Pattern replayidpattern = Pattern.compile(replayidpatternStr);

	/**
	 * @param replayid2
	 * @return
	 */
	public static String getCollectionFromReplayId(String replayid) {		
		Matcher m = replayidpattern.matcher(replayid);
		if (m.find()) {
			return m.group(1);
		} else {
			LOGGER.error(String.format("Not able to extract collection from replay id %s", replayid));
			return replayid;
		}
	}
	
	public static String getReplayIdFromCollection(String collection) {
		return String.format("%s-%s", collection, UUID.randomUUID().toString());
	}

	/**
	 * @return
	 */
	@JsonIgnore
	public List<Request> getRequests() {
		return rrstore.getRequests(customerid, app, collection, reqids, paths, RRBase.RR.Record);
	}
}
