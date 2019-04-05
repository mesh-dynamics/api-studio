/*
 *
 *    Copyright Cube I O
 *
 */
package com.cube.dao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
	 * @param reqids
	 * @param replayid
	 * @param async
	 * @param status
	 * @param samplerate
	 */
	public Replay(String endpoint, String customerid, String app, String instanceid, String collection, List<String> reqids,
				  String replayid, boolean async, ReplayStatus status,
				  List<String> paths, int reqcnt, int reqsent, int reqfailed, String creationTimestamp,
				  Optional<Double> samplerate) {
		super();
		this.endpoint = endpoint;
		this.customerid = customerid;
		this.app = app;
		this.instanceid = instanceid;
		this.collection = collection;
		this.reqids = reqids;
		this.replayid = replayid;
		this.async = async;
		this.status = status;
		this.paths = paths;
		this.reqcnt = reqcnt;
		this.reqsent = reqsent;
		this.reqfailed = reqfailed;
		this.creationTimeStamp = creationTimestamp == null ? format.format(new Date()) : creationTimestamp;
		this.xfmer = Optional.ofNullable(null);
		this.samplerate = samplerate;
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


	public final String endpoint;
	public final String customerid;
	public final String app;
	public final String instanceid;
	public final String collection;
	public final List<String> reqids;
	public final String replayid; // this needs to be globally unique
	public final boolean async;
	public ReplayStatus status;
	public final List<String> paths; // paths to be replayed
	public int reqcnt; // total number of requests
	public int reqsent; // number of requests sent. Some requests could be skipped due to exceptions
	public int reqfailed; // requests failed, return code not 200
	public Optional<RRTransformer> xfmer;
	public final Optional<Double> samplerate;

	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	public final String creationTimeStamp;

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
	public Stream<List<Request>> getRequestBatches(int batchSize, ReqRespStore rrstore) {
		Result<Request> requests = getRequests(rrstore);
		
		return BatchingIterator.batchedStreamOf(requests.getObjects(), batchSize);
	}

}
