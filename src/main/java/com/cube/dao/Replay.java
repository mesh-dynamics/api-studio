/*
 *
 *    Copyright Cube I O
 *
 */
package com.cube.dao;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.core.ReplayTypeEnum;
import io.md.dao.Event;
import io.md.dao.Event.EventType;

import com.cube.core.BatchingIterator;
import com.cube.core.RRTransformer;
import com.cube.utils.Constants;

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
		Error,
        Analyzing,
        AnalyzeComplete
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
	public Replay(String endpoint, String customerId, String app, String instanceId,
		String collection, String userId, List<String> reqIds,
		String replayId, boolean async, String templateVersion, ReplayStatus status,
		List<String> paths, int reqcnt, int reqsent, int reqfailed, Instant creationTimestamp,
		Optional<Double> sampleRate, List<String> intermediateServices,
		Optional<String> generatedClassJarPath, Optional<URLClassLoader> classLoader,
		Optional<String> service, ReplayTypeEnum replayType, Optional<String> xfms, Optional<RRTransformer> xfmer, List<String> mockServices) {
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
		this.creationTimeStamp = creationTimestamp;
		this.xfms = xfms;
		this.xfmer = xfmer;
		this.sampleRate = sampleRate;
		this.intermediateServices = intermediateServices;
		this.generatedClassJarPath = generatedClassJarPath;
		this.service = service;
		this.replayType = replayType;
		this.generatedClassLoader = classLoader;
		this.mockServices = mockServices;
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
	    service = Optional.empty();
	    intermediateServices = Collections.emptyList();
	    templateVersion = "";
	    generatedClassJarPath = Optional.empty();
	    replayType = ReplayTypeEnum.HTTP;
	    xfms = Optional.empty();
	    xfmer = Optional.empty();
	    mockServices = Collections.emptyList();
    }

	/*
	 * @param jsonStrRepOfXfms: multivalued map of {key : [{src, tgt}+]} in a string representation
	 */
	/*
	public void updateXfmsFromJSONString(String jsonStrRepOfXfms) throws JSONException {
		JSONObject obj = new JSONObject(jsonStrRepOfXfms);
		if (xfmer.isPresent()) {
			xfmer.get().updateTransforms(obj);
		} else {
			xfmer = Optional.of(new RRTransformer(obj));
		}
	}
	*/

	@JsonProperty("endpoint")
	public final String endpoint;
	@JsonProperty("customerId")
	public final String customerId;
    @JsonProperty("app")
	public final String app;
    @JsonProperty("instanceId")
	public final String instanceId;
    @JsonProperty("collection")
	public final String collection;
    @JsonProperty("userId")
    public final String userId;
    @JsonProperty("reqIds")
	public final List<String> reqIds;
    @JsonProperty("templateVersion")
	public final String templateVersion;
    @JsonProperty("replayId")
    public final String replayId; // this needs to be globally unique
    @JsonProperty("async")
    public final boolean async;
    @JsonProperty("status")
	public ReplayStatus status;
    @JsonProperty("service")
    public final Optional<String> service;
    @JsonProperty("paths")
    public final List<String> paths; // paths to be replayed
    @JsonProperty("intermediateServices")
    public final List<String> intermediateServices;
    @JsonProperty("reqcnt")
    public int reqcnt; // total number of requests
    @JsonProperty("reqsent")
    public int reqsent; // number of requests sent. Some requests could be skipped due to exceptions
    @JsonProperty("reqfailed")
    public int reqfailed; // requests failed, return code not 200
    public final Optional<String> xfms; // the transformation string
	@JsonIgnore
    public final Optional<RRTransformer> xfmer;
    @JsonProperty("sampleRate")
	public final Optional<Double> sampleRate;
	@JsonProperty("creationTimeStamp")
    public final Instant creationTimeStamp;
	@JsonProperty("generatedClassJarPath")
	public Optional<String> generatedClassJarPath;
	@JsonProperty("replayType")
	public final ReplayTypeEnum replayType;
	@JsonProperty("mockServices")
    public final List<String> mockServices;
	public transient Optional<URLClassLoader> generatedClassLoader;

	@JsonSetter
	public void setGeneratedClassJarPath(Optional<String> jarPathOpt){
		this.generatedClassJarPath = jarPathOpt;
		generatedClassJarPath.ifPresent(jarPath -> {
			try {
				Path path = Paths.get(jarPath);
				this.generatedClassLoader = Optional.of(new URLClassLoader(
					new URL[]{path.toUri().toURL()},
					this.getClass().getClassLoader()
				));
			} catch (Exception e) {
				LOGGER.error(new
					ObjectMessage(Map.of(Constants.MESSAGE, "Unable to initialize URL Class Loader",
					Constants.JAR_PATH_FIELD, jarPath)));
			}
		});
	}


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

	public Pair<Stream<List<Event>>, Long> getRequestBatchesUsingEvents(int batchSize, ReqRespStore rrstore,
                                                                          ObjectMapper jsonMapper) {
        Result<Event> requests = getEventResult(rrstore);
        return Pair.of(BatchingIterator.batchedStreamOf(requests.getObjects(), batchSize), requests.numFound);
    }

	private Result<Event> getEventResult(ReqRespStore rrstore) {
		EventQuery eventQuery = new EventQuery.Builder(customerId, app, EventType.fromReplayType(replayType))
			.withRunType(Event.RunType.Record).withReqIds(reqIds).withPaths(paths)
			.withCollection(collection)
			.withServices(service.map(List::of).orElse(Collections.emptyList())).withSortOrderAsc(true).build();
		return rrstore.getEvents(eventQuery);
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
