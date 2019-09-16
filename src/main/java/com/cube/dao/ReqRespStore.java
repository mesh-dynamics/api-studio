/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.cube.agent.FnReqResponse;
import com.cube.agent.FnResponse;
import com.cube.cache.ReplayResultCache.ReplayPathStatistic;
import com.cube.cache.TemplateKey;
import com.cube.core.Comparator;
import com.cube.core.CompareTemplate;
import com.cube.core.RequestComparator;
import com.cube.dao.Analysis.ReqRespMatchResult;
import com.cube.dao.Recording.RecordingStatus;
import com.cube.dao.Replay.ReplayStatus;
import com.cube.golden.TemplateSet;
import com.cube.golden.TemplateUpdateOperationSet;

/**
 * @author prasad
 *
 */
public interface ReqRespStore {


    Optional<TemplateSet> getTemplateSet(String customerid, String app, String version);


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
        Event,
		Request,
		Response,
		ReplayMeta, // replay metadata
		Analysis,
		ReqRespMatchResult,
		Recording,
		ResponseCompareTemplate,
		RequestCompareTemplate,
		ReplayStats,
        FuncReqResp,
        TemplateSet,
        TemplateUpdateOperationSet,
        GoldenSet,
        RecordingOperationSetMeta,
        RecordingOperationSet
    }

	boolean save(Request req);

	boolean save(Response resp);

    boolean save(Event event);

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
	String saveCompareTemplate(TemplateKey key, String templateAsJson);

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
    Stream<Replay> getReplay(Optional<String> customerid, Optional<String> app, List<String> instanceid,
                             List<ReplayStatus> status, Optional<Integer> numOfResults, Optional<String> collection);


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
                             List<ReplayStatus> status, Optional<Integer> numOfResults, Optional<String> collection);

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

    Optional<Recording> getRecording(String recordingId);


	/**
	 * @param customerid
	 * @param app
	 * @param instanceid
	 * @return
	 */
	Optional<String> getCurrentCollection(Optional<String> customerid, Optional<String> app,
			Optional<String> instanceid);

    /**
     *
     * @param customerId
     * @param app
     * @param instanceId
     * @return
     */
	Optional<String> getCurrentReplayId(Optional<String> customerId, Optional<String> app,
                                        Optional<String> instanceId);


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
	Optional<Recording> getRecordingByCollectionAndTemplateVer(String customerid, String app,
                                                               String collection, Optional<String> templateSetVersion);



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

        @JsonIgnore
		public Optional<String> getCollection() {
			// Note that replayid is the collection for replay requests/responses
			// replay.collection refers to the original collection
			// return replay collection if non empty, else return recording collection
			return replay.map(replay -> replay.replayid)
					.or(() -> recording.map(recording -> recording.collection));
		}

		@JsonIgnore
		public Optional<String> getRecordingCollection() {
			// return collection of recording corresponding to replay if non empty, else return recording collection
			return replay.map(replay -> replay.collection)
					.or(() -> recording.map(recording -> recording.collection));
		}

		@JsonIgnore
        public Optional<String> getReplayId() {
            return replay.map(replay -> replay.replayid);
        }

		@JsonIgnore
		public boolean isRecording() {
			return recording.isPresent();
		}

		@JsonIgnore
        public Optional<String> getTemplateVersion() {
            return replay.flatMap(replay1 -> replay1.templateVersion)
                .or(() -> recording.flatMap(recording1 -> recording1.templateVersion));
        }

		// for json de-serialization
		public RecordOrReplay() {
		    super();
		    replay = Optional.empty();
		    recording = Optional.empty();
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

		private RecordOrReplay(Replay replay) {
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

		@Override
        public String toString() {
		    return getCollection().or(() -> getRecordingCollection()).orElse("N/A");
        }

        @JsonProperty("recording")
		public final Optional<Recording> recording;
		@JsonProperty("replay")
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
     * Get ReqResponseMatchResult for the given replay Id, record req id and replay req id
     * It matches on both record and replay reqid. If any of them are empty, it requires the matching result to also
     * have empty value for that field
     * @param recordReqId
     * @param replayId
     * @return
     */
    Optional<ReqRespMatchResult> getAnalysisMatchResult(Optional<String> recordReqId, Optional<String> replayReqId,
                                                        String replayId);


    /**
     * Get results matching a path and other constraints
     * @param replayId
     * @param service
     * @param path
     * @param reqmt
     * @param respmt
     * @param start
     * @param nummatches
     * @return
     */
    Result<ReqRespMatchResult>
    getAnalysisMatchResults(String replayId, Optional<String> service, Optional<String> path, Optional<Comparator.MatchType> reqmt,
                            Optional<Comparator.MatchType> respmt, Optional<Integer> start, Optional<Integer> nummatches);


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

	boolean saveFnReqRespNewCollec(String customer, String app, String collection, String newCollection);

    /**
     * TODO instead of the source template set (maybe we want to specify source golden set id/version)
     * Create a new template update operation set
     * @param customer Customer
     * @param app Application Name
     * @param sourceTemplateSetVersion The version of the source template set (stand in for id) on which these
     *                                 operations will be applied
     * @return Id of the newly constructed template update operation set
     */
	String createTemplateUpdateOperationSet(String customer, String app, String sourceTemplateSetVersion) throws Exception;

    /**
     * Save an update template update operation set
     * @param templateUpdateOperationSet Updated template update operation set
     * @return success flag
     */
    boolean saveTemplateUpdateOperationSet(TemplateUpdateOperationSet templateUpdateOperationSet) throws Exception;

    /**
     * Fetch a template update operation set given id
     * @param templateUpdateOperationSetId Template Update Operation Set Id
     * @return The corresponding operation set object
     */
	Optional<TemplateUpdateOperationSet> getTemplateUpdateOperationSet(String templateUpdateOperationSetId);

	//boolean updateCollection(Recording sourceRecording, List<ReqRespUpdateOperation> recordingUpdateSpec);

    /**
     * Save a template set
     * @param templateSet Template Set
     * @return success flag
     */
	String saveTemplateSet(TemplateSet templateSet) throws Exception;

    Optional<TemplateSet> getTemplateSet(String templateSetId);

    Optional<TemplateSet> getLatestTemplateSet(String customer, String app);

    public void invalidateCurrentCollectionCache(String customerid, String app,
                                                 String instanceid);

/*    String createGoldenSet(String collection, String templateSetId , Optional<String> parentGoldenSet, Optional<String> rootGoldenSet);

    Optional<GoldenSet> getGoldenSet(String goldenSetId) throws Exception;

    Stream<GoldenSet> getGoldenSetStream(Optional<String> customer, Optional<String> app, Optional<String> instanceid);

    Stream<GoldenSet> getAllDerivedGoldenSets(String rootGoldentSetId);*/

}
