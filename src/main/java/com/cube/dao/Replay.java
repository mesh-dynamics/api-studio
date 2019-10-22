/*
 *
 *    Copyright Cube I O
 *
 */
package com.cube.dao;

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
     * @param customerid
     * @param app
     * @param instanceid
     * @param collection
     * @param userid
     * @param reqids
     * @param replayid
     * @param async
     * @param templateVersion
     * @param status
     * @param samplerate
     */
	public Replay(String endpoint, String customerid, String app, String instanceid, String collection, String userid, List<String> reqids,
                  String replayid, boolean async, String templateVersion, ReplayStatus status,
                  List<String> paths, int reqcnt, int reqsent, int reqfailed, Instant creationTimestamp,
                  Optional<Double> samplerate, List<String> intermediateServices) {
		super();
		this.endpoint = endpoint;
		this.customerid = customerid;
		this.app = app;
		this.instanceid = instanceid;
		this.collection = collection;
		this.userid = userid;
		this.reqids = reqids;
		this.replayid = replayid;
		this.async = async;
        this.templateVersion = templateVersion;
        this.status = status;
		this.paths = paths;
		this.reqcnt = reqcnt;
		this.reqsent = reqsent;
		this.reqfailed = reqfailed;
		this.creationTimeStamp = creationTimestamp == null ? Instant.now() : creationTimestamp;
		this.xfmer = Optional.ofNullable(null);
		this.samplerate = samplerate;
		this.intermediateServices = intermediateServices;
	}

	//for deserialization
	public Replay() {
	    super();
	    endpoint = "" ;
	    customerid = "";
	    app = "";
	    instanceid = "";
	    collection = "";
	    userid = "";
	    replayid = "";
	    async = false;
	    samplerate = Optional.empty();
	    creationTimeStamp = Instant.now();
	    reqids = Collections.emptyList();
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
	public final String customerid;
    @JsonProperty("app")
	public final String app;
    @JsonProperty("instance")
	public final String instanceid;
    @JsonProperty("collect")
	public final String collection;
    @JsonProperty("userid")
    public final String userid;
    @JsonProperty("reqids")
	public final List<String> reqids;
    @JsonProperty("templateVer")
	public final String templateVersion;
    @JsonProperty("id")
    public final String replayid; // this needs to be globally unique
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
	public final Optional<Double> samplerate;
	@JsonProperty("timestmp")
    public final Instant creationTimeStamp;

	static final String uuidpatternStr = "\\b[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-\\b[0-9a-fA-F]{12}\\b";
	static final String replayidpatternStr = "^(.*)-" + uuidpatternStr + "$";
	private static final Pattern replayidpattern = Pattern.compile(replayidpatternStr);

	/**
	 * @param replayid
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
	public Result<Request> getRequests(ReqRespStore rrstore) {
		Result<Request> res = rrstore.getRequests(customerid, app, collection, reqids, paths, RRBase.RR.Record);
		return res;
	}

	@JsonIgnore
	public Pair<Stream<List<Request>>, Long> getRequestBatches(int batchSize, ReqRespStore rrstore) {
		Result<Request> requests = getRequests(rrstore);
		return Pair.of(BatchingIterator.batchedStreamOf(requests.getObjects(), batchSize), requests.numFound);
	}

}
