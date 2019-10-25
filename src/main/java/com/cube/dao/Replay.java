/*
 *
 *    Copyright Cube I O
 *
 */
package com.cube.dao;

import com.cube.dao.Event.EventType;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.spotify.docker.client.shaded.com.fasterxml.jackson.annotation.JsonProperty;

import com.cube.core.BatchingIterator;
import com.cube.core.RRTransformer;

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
     * @param customerId
     * @param app
     * @param instanceId
     * @param collection
     * @param userId
     * @param reqIds
     * @param replayId
     * @param async
     * @param templateVersion
     * @param status
     * @param sampleRate
     */
	public Replay(String endpoint, String customerId, String app, String instanceId, String collection, String userId
            , List<String> reqIds,
                  String replayId, boolean async, String templateVersion, ReplayStatus status,
                  List<String> paths, int reqcnt, int reqsent, int reqfailed, Instant creationTimestamp,
                  Optional<Double> sampleRate, List<String> intermediateServices) {
		super();
		this.endpoint = endpoint;
		this.customerId = customerId;
		this.app = app;
		this.instanceId = instanceId;
		this.collection = collection;
        this.userId = userId;
		this.reqIds = reqIds;
		this.replayId = replayId;
		this.async = async;
        this.templateVersion = templateVersion;
        this.status = status;
		this.paths = paths;
		this.reqcnt = reqcnt;
		this.reqsent = reqsent;
		this.reqfailed = reqfailed;
		this.creationTimeStamp = creationTimestamp == null ? Instant.now() : creationTimestamp;
		this.xfmer = Optional.ofNullable(null);
		this.sampleRate = sampleRate;
		this.intermediateServices = intermediateServices;
	}

	//for deserialization
	public Replay() {
	    super();
	    endpoint = "" ;
	    customerId = "";
	    app = "";
	    instanceId = "";
	    collection = "";
	    userId = "";
	    replayId = "";
	    async = false;
	    sampleRate = Optional.empty();
        creationTimeStamp = Instant.now();
	    reqIds = Collections.emptyList();
	    paths = Collections.emptyList();
	    intermediateServices = Collections.emptyList();
	    templateVersion = "";
    }

	/*
	 * @param jsonStrRepOfXfms: multivalued map of {key : [{src, tgt}+]} in a string representation
	 */
	public void updateXfmsFromJSONString(String jsonStrRepOfXfms) throws JSONException {
		JSONObject obj = new JSONObject(jsonStrRepOfXfms);
		if (xfmer.isPresent()) {
			xfmer.get().updateTransforms(obj);
		} else {
			xfmer = Optional.of(new RRTransformer(obj));
		}
	}

	@JsonProperty("endpt")
	public final String endpoint;
	@JsonProperty("cust")
	public final String customerId;
    @JsonProperty("app")
	public final String app;
    @JsonProperty("instance")
	public final String instanceId;
    @JsonProperty("collect")
	public final String collection;
    @JsonProperty("userid")
    public final String userId;
    @JsonProperty("reqIds")
	public final List<String> reqIds;
    @JsonProperty("templateVer")
	public final String templateVersion;
    @JsonProperty("id")
    public final String replayId; // this needs to be globally unique
    @JsonProperty("async")
    public final boolean async;
    @JsonProperty("status")
	public ReplayStatus status;
    @JsonProperty("paths")
    public final List<String> paths; // paths to be replayed
    @JsonProperty("intermdtserv")
    public final List<String> intermediateServices;
    @JsonProperty("reqcnt")
    public int reqcnt; // total number of requests
    @JsonProperty("reqsnt")
    public int reqsent; // number of requests sent. Some requests could be skipped due to exceptions
    @JsonProperty("reqfl")
    public int reqfailed; // requests failed, return code not 200
	@JsonIgnore
    public transient Optional<RRTransformer> xfmer;
    @JsonProperty("smplrate")
	public final Optional<Double> sampleRate;
	@JsonProperty("timestmp")
    public final Instant creationTimeStamp;

	static final String uuidPatternStr = "\\b[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-\\b[0-9a-fA-F]{12}\\b";
	static final String replayIdPatternStr = "^(.*)-" + uuidPatternStr + "$";
	private static final Pattern replayIdPattern = Pattern.compile(replayIdPatternStr);

	/**
	 * @param replayId
	 * @return
	 */
	public static String getCollectionFromReplayId(String replayId) {
		Matcher m = replayIdPattern.matcher(replayId);
		if (m.find()) {
			return m.group(1);
		} else {
			LOGGER.error(String.format("Not able to extract collection from replay id %s", replayId));
			return replayId;
		}
	}

	public static String getReplayIdFromCollection(String collection) {
		return String.format("%s-%s", collection, UUID.randomUUID().toString());
	}

	public Pair<Stream<List<Request>>, Long> getRequestBatchesUsingEvents(int batchSize, ReqRespStore rrstore,
                                                                          ObjectMapper jsonMapper) {
        Result<Request> requests = mapEventToRequestResult(getEventResult(rrstore), jsonMapper);
        return Pair.of(BatchingIterator.batchedStreamOf(requests.getObjects(), batchSize), requests.numFound);
    }

    private Result<Request> mapEventToRequestResult(Result<Event> events, ObjectMapper jsonMapper) {
        return new Result<>(events.getObjects()
                .flatMap(event -> Request.fromEvent(event, jsonMapper).stream()), events.numResults, events.numFound);
    }

    private Result<Event> getEventResult(ReqRespStore rrstore) {
        EventQuery eventQuery = new EventQuery.Builder(customerId, app, EventType.HTTPRequest)
            .withRunType(Event.RunType.Record).withReqIds(reqIds).withPaths(paths).withCollection(collection).build();
        return rrstore.getEvents(eventQuery);
    }

    /**
	 * @return
	 */
	@JsonIgnore
	public Result<Request> getRequests(ReqRespStore rrstore) {
		Result<Request> res = rrstore.getRequests(customerId, app, collection, reqIds, paths, Event.RunType.Record);
		return res;
	}

	@JsonIgnore
	public Pair<Stream<List<Request>>, Long> getRequestBatches(int batchSize, ReqRespStore rrstore) {
		Result<Request> requests = getRequests(rrstore);
		return Pair.of(BatchingIterator.batchedStreamOf(requests.getObjects(), batchSize), requests.numFound);
	}

    @JsonIgnore
    public Pair<Stream<List<Event>>, Long> getRequestEventBatches(int batchSize, ReqRespStore rrstore) {
	    EventQuery.Builder builder = new EventQuery.Builder(customerId, app, Event.getRequestEventTypes());
	    EventQuery eventQuery = builder.withCollection(collection)
            .withReqIds(reqIds)
            .withPaths(paths)
            .withRunType(Event.RunType.Record).build();
        Result<Event> requests = rrstore.getEvents(eventQuery);
        return Pair.of(BatchingIterator.batchedStreamOf(requests.getObjects(), batchSize), requests.numFound);
    }

}
