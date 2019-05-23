/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


import io.cube.agent.FnResponse;

import com.cube.agent.FnReqResponse;
import com.cube.cache.ReplayResultCache.ReplayPathStatistic;
import com.cube.cache.TemplateKey;
import com.cube.core.CompareTemplate;
import com.cube.core.RequestComparator;
import com.cube.dao.Analysis.ReqRespMatchResult;
import com.cube.dao.Recording.RecordingStatus;
import com.cube.dao.Replay.ReplayStatus;

/**
 * @author prasad
 *
 */
public interface ReqRespStore {


	public class ReqResp {
				
		
		/**
		 * @param pathwparams
		 * @param meta
		 * @param hdrs
		 * @param body
		 */
		private ReqResp(String pathwparams, List<Map.Entry<String, String>> meta,
				List<Map.Entry<String, String>> hdrs, String body) {
			super();
			this.pathwparams = pathwparams;
			this.meta = meta;
			this.hdrs = hdrs;
			this.body = body;
		}
		
		/**
		 * 
		 */
		private ReqResp() {
			super();
			this.pathwparams = "";
			this.meta = new ArrayList<Map.Entry<String, String>>();
			this.hdrs = new ArrayList<Map.Entry<String, String>>();
			this.body = "";
		}


		@JsonProperty("path")
		public final String pathwparams; // path with params
        @JsonDeserialize(as=ArrayList.class)
		public final List<Map.Entry<String, String>> meta;
        @JsonDeserialize(as=ArrayList.class)
		public final List<Map.Entry<String, String>> hdrs;
		public final String body;
		
	}

	enum Types {
		Request,
		Response,
		ReplayMeta, // replay metadata
		Analysis,
		ReqRespMatchResult,
		Recording,
		ResponseCompareTemplate,
		RequestCompareTemplate,
		ReplayStats,
        FuncReqResp
	}

	boolean save(Request req);
	
	boolean save(Response resp);
		
	/**
	 * @param queryrequest
	 * @param mspec - the matching specification
	 * @param nummatches - max number of matches
	 * @return the requests matching queryrequest based on the matching spec
	 */
	Stream<Request> getRequests(Request queryrequest, RequestComparator mspec, Optional<Integer> nummatches);

    /**
     * @param queryrequest
     * @param mspec - the matching specification
     * @param nummatches - max number of matches
     * @param start - skip the first "start" number of matches (for paging)
     * @return the requests matching queryrequest based on the matching spec
     */
    Stream<Request> getRequests(Request queryrequest, RequestComparator mspec, Optional<Integer> nummatches,
                                Optional<Integer> start);

    /**
	 * @param reqid
	 * @return the matching response on the reqid 
	 */
	Optional<Response> getResponse(String reqid);

    /**
     * @param reqid
     * @return the matching response on the reqid
     */
    Optional<Request> getRequest(String reqid);

	/**
	 * @param requests
	 * @return
	 */
	Map<String, Response> getResponses(List<Request> requests);

	/**
	 * @param queryrequest
	 * @return the response corresponding to the request matching in the db
	 * to find the matching request, the reqid field of queryrequest is ignored
	 */
	Optional<Response> getRespForReq(Request queryrequest, RequestComparator mspec);

	
	/**
	 * @param customerid
	 * @param app
	 * @param collection
	 * @param reqids
	 * @param paths 
	 * @param rrtype
	 * @return
	 */
	Result<Request> getRequests(String customerid, String app, String collection, List<String> reqids
			, List<String> paths, RRBase.RR rrtype);

	/**
	 * @param replay
	 * @return 
	 */
	boolean saveReplay(Replay replay);

	/**
	 * @param replayid
	 * @return
	 */
	Optional<Replay> getReplay(String replayid);


	/**
	 * Save an analysis template for the given parameters in the backend
	 * database
	 * @param key
	 * @param templateAsJson
	 * @return
	 */
	boolean saveCompareTemplate(TemplateKey key, String templateAsJson);

	/**
	 * Retrieve an analysis template from the database for
	 * the given parameters
	 * @param key
	 * @return
	 */
	Optional<CompareTemplate> getCompareTemplate(TemplateKey key);


    /**
     *
     * @param customerid
     * @param app
     * @param instanceid
     * @param status
     * @param collection
     * @param numOfResults
     * @return
     */
	Stream<Replay> getReplay(Optional<String> customerid, Optional<String> app, Optional<String> instanceid,
                             ReplayStatus status, Optional<Integer> numOfResults, Optional<String> collection);

	/**
     * @param customerid
     * @param app
     * @param instanceid
     * @param status
     * @return
     */
    Stream<Replay> getReplay(Optional<String> customerid, Optional<String> app,
                             Optional<String> instanceid, ReplayStatus status);

	static void main(String[] args) throws IOException{

		Map.Entry<String, String> e = new AbstractMap.SimpleEntry<String, String>("k1", "v1");
		ObjectMapper m1 = new ObjectMapper();
		String jr = m1.writerWithDefaultPrettyPrinter()
				  .writeValueAsString(e);

		System.out.println(String.format("Json string: %s", jr));
		
		TypeReference<Map.Entry<String, String>> tR 
		  = new TypeReference<Map.Entry<String, String>>() {};
		Map.Entry<String, String> e2 = m1.readValue(jr, tR);
		System.out.println("Object read back: " + e2.toString());
		
		
		List<Map.Entry<String, String>> meta = new ArrayList<AbstractMap.Entry<String, String>>();
		meta.add(new SimpleEntry<String, String>("m1", "m1v1"));
		meta.add(new SimpleEntry<String, String>("m1", "m1v2"));
		meta.add(new SimpleEntry<String, String>("m2", "m2v1"));
		meta.add(new SimpleEntry<String, String>("m2", "m2v1"));
		
		List<Map.Entry<String, String>> hdrs = new ArrayList<AbstractMap.Entry<String, String>>();
		hdrs.add(new SimpleEntry<String, String>("h1", "h1v1"));
		hdrs.add(new SimpleEntry<String, String>("h1", "h1v2"));
		hdrs.add(new SimpleEntry<String, String>("h2", "h2v1"));
		hdrs.add(new SimpleEntry<String, String>("h2", "h2v1"));
		
		ReqResp rr = new ReqResp("/p1?a=av", meta, hdrs, "body 1");
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonResult = mapper.writerWithDefaultPrettyPrinter()
		  .writeValueAsString(rr);

		System.out.println(String.format("Json string: %s", jsonResult));
		
		TypeReference<ReqResp> typeRef 
		  = new TypeReference<ReqResp>() {};
		ReqResp rr2 = mapper.readValue(jsonResult, typeRef);
		System.out.println("Object read back: " + rr2.toString());

		String jsonResult2 = mapper.writerWithDefaultPrettyPrinter()
				  .writeValueAsString(rr2);

		System.out.println(String.format("Json string: %s", jsonResult2));

		
	}

	/**
	 * @param analysis
	 * @return
	 */
	boolean saveAnalysis(Analysis analysis);

	/**
	 * @param res
	 * @return 
	 */
	boolean saveResult(ReqRespMatchResult res);

	/**
	 * @param replayid
	 * @return
	 */
	Optional<Analysis> getAnalysis(String replayid);

	/**
	 * @param customerid
	 * @param app
	 * @param instanceid 
	 * @param status
	 * @return
	 */
	Stream<Recording> getRecording(Optional<String> customerid, Optional<String> app, 
			Optional<String> instanceid, Optional<RecordingStatus> status);


	
	/**
	 * @param customerid
	 * @param app
	 * @param instanceid
	 * @return
	 */
	Optional<String> getCurrentCollection(Optional<String> customerid, Optional<String> app,
			Optional<String> instanceid);

	/**
	 * @param customerid
	 * @param app
	 * @param instanceid
	 * @return For both record and replay, return the collection of the record stage
	 */
	Optional<String> getCurrentRecordingCollection(Optional<String> customerid, Optional<String> app,
			Optional<String> instanceid);

	
	/**
	 * @param recording
	 * @return
	 */
	boolean saveRecording(Recording recording);

	/**
	 * @param customerid
	 * @param app
	 * @param collection
	 * @return
	 */
	Optional<Recording> getRecordingByCollection(String customerid, String app,
			String collection);
	
	
	
	/**
	 * @param replayid
	 * @param service
	 * @return If service is empty, return aggregate results for all services. If 
	 * service is non-empty, return results for all paths in the service if bypath is true
	 * This also returns the rollups (service, path), (service) ()
	 */
	Collection<MatchResultAggregate> getResultAggregate(String replayid, Optional<String> service,
			boolean bypath);

	/**
	 * @param customerid
	 * @param app
	 * @param instanceid
	 * @return
	 */
	Optional<RecordOrReplay> getCurrentRecordOrReplay(Optional<String> customerid, Optional<String> app,
			Optional<String> instanceid);

	/**
	 * 
	 */
	boolean commit();

	class RecordOrReplay {


		public Optional<String> getCollection() {
			// Note that replayid is the collection for replay requests/responses
			// replay.collection refers to the original collection
			// return replay collection if non empty, else return recording collection
			return replay.map(replay -> replay.replayid)
					.or(() -> recording.map(recording -> recording.collection));
		}

		public Optional<String> getRecordingCollection() {
			// return collection of recording corresponding to replay if non empty, else return recording collection
			return replay.map(replay -> replay.collection)
					.or(() -> recording.map(recording -> recording.collection));
		}

		public boolean isRecording() {
			return recording.isPresent();
		}

		/**
		 *
		 */
		private RecordOrReplay(Optional<Recording> recording, Optional<Replay> replay) {
			super();
			this.replay = replay;
			this.recording = recording;
		}

		private RecordOrReplay(Recording recording) {
			this(Optional.of(recording), Optional.empty());
		}

		public RecordOrReplay(Replay replay) {
			this(Optional.empty(), Optional.of(replay));
		}

		public static RecordOrReplay createFromRecording(Recording recording) {
			RecordOrReplay rr = new RecordOrReplay(recording);
			return rr;
		}

		public static RecordOrReplay createFromReplay(Replay replay) {
			RecordOrReplay rr = new RecordOrReplay(replay);
			return rr;
		}

		public final Optional<Recording> recording;
		public final Optional<Replay> replay;
	}

	/**
	 * Get ReqResponseMatchResult for the given request and replay Id
	 * @param recordReqId
	 * @param replayId
	 * @return
	 */
	Optional<ReqRespMatchResult> getAnalysisMatchResult(String recordReqId , String replayId);

	/**
	 * Save replay results (request match / not match counts) for a given customer/app/virtual(mock) service
	 * combination. The counts are stored in the backend path wise.
	 * @param pathStatistics
	 * @param replayId
	 */
	void saveReplayResult(Map<String , List<ReplayPathStatistic>> pathStatistics, String replayId);

	/**
	 * Get replay results (request match / not match counts) from the backend for a given customer/app/virtual(mock)
	 * service and replay combination
	 * @param customer
	 * @param app
	 * @param service
	 * @param replayId
	 * @return
	 */
	List<String> getReplayRequestCounts(String customer, String app, String service, String replayId);

	/**
	 * Given a request List, fetch requests from backend matching on trace id's of the original requests and belonging
	 * to the service name list provided
	 * @param requestList
	 * @param intermediateServices
	 * @param collectionId
	 * @return
	 */
	Stream<Request> expandOnTraceId(List<Request> requestList, List<String> intermediateServices
			, String collectionId);

	/**
	 * Given a request Id , find all the ReqRespMatchResults for Requests having the same traceId
	 * as the given request in the same collection as Request (This function can be used for getting match results for
	 * both record and replay)
     * Note that, a recorded request and a replayed request will have the same trace id (as we force that during replay).
     * However keeping a provision to expand recorded request and replayed request separately on their traces as
     * we might change the logic later, if we require traces to be different during record and replay
     * @param reqRespMatchResult
	 * @param recordingOrReplay
	 * @return
	 */
	Stream<ReqRespMatchResult> expandOnTrace(ReqRespMatchResult reqRespMatchResult, boolean recordingOrReplay);

	boolean storeFunctionReqResp(FnReqResponse funcReqResponse, String collection);

	Optional<FnResponse> getFunctionReturnValue(FnReqResponse funcReqResponse, String collection);
}
