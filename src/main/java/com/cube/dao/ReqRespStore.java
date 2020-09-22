/**
 * Copyright Cube I O
 */
package com.cube.dao;

import io.md.core.ConfigApplicationAcknowledge;
import io.md.dao.Event.EventType;
import io.md.dao.agent.config.AgentConfigTagInfo;
import io.md.dao.agent.config.ConfigDAO;
import java.io.IOException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.solr.common.util.Pair;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.cube.agent.FnReqResponse;
import io.md.constants.ReplayStatus;
import io.md.core.AttributeRuleMap;
import io.md.core.Comparator;
import io.md.core.CompareTemplate;
import io.md.core.TemplateKey;
import io.md.dao.Event;
import io.md.dao.Event.RunType;
import io.md.dao.EventQuery;
import io.md.dao.RecordOrReplay;
import io.md.dao.Recording;
import io.md.dao.Recording.RecordingStatus;
import io.md.dao.RecordingOperationSetSP;
import io.md.dao.Replay;
import io.md.dao.ReqRespMatchResult;
import io.md.dao.Analysis;
import io.md.services.DataStore;
import io.md.services.FnResponse;
import io.md.injection.DynamicInjectionConfig;
import io.md.utils.Constants;

import com.cube.dao.ReqRespStoreImplBase.CollectionKey;
import com.cube.dao.ReqRespStoreSolr.ReqRespResultsWithFacets;
import com.cube.dao.ReqRespStoreSolr.SolrStoreException;
import com.cube.golden.TemplateSet;
import com.cube.golden.TemplateUpdateOperationSet;

/**
 * @author prasad
 *
 */
public interface ReqRespStore extends DataStore {

    Logger LOGGER = LogManager.getLogger(ReqRespStore.class);

    static Optional<Recording> startRecording(Recording recording, ReqRespStore rrstore) {
        if (rrstore.saveRecording(recording)) {
        	rrstore.populateCache(
        		new CollectionKey(recording.customerId, recording.app, recording.instanceId),
		        RecordOrReplay.createFromRecording(recording));
            return Optional.of(recording);
        }
        return Optional.empty();
    }

    static Recording stopRecording(Recording recording, ReqRespStore rrstore) {
		if (recording.status == RecordingStatus.Running) {
			LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Stopping recording",
				Constants.RECORDING_ID, recording.id)));
			recording.status = RecordingStatus.Completed;
			recording.updateTimestamp = Optional.of(Instant.now());
			rrstore.expireRecordingInCache(recording);
		}
		return recording;
	}

	static Recording forceStopRecording(Recording recording, ReqRespStore rrstore) {
    	if (recording.status == RecordingStatus.Running) {
    		recording.status = RecordingStatus.Completed;
		    recording.updateTimestamp = Optional.of(Instant.now());
		    rrstore.forceDeleteInCache(recording);
		    rrstore.saveRecording(recording);
	    }
		return recording;
    }

    static Recording resumeRecording(Recording recording, ReqRespStore rrstore) {
        if (recording.status != RecordingStatus.Running) {
            LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Resuming recording",
                Constants.RECORDING_ID, recording.id)));
            recording.status = RecordingStatus.Running;
            recording.updateTimestamp = Optional.of(Instant.now());
            rrstore.saveRecording(recording);
        }
        return recording;
    }

    static Recording softDeleteRecording(Recording recording, ReqRespStore rrstore)
		throws Recording.RecordingSaveFailureException {
		recording.archived = true;
		recording.updateTimestamp = Optional.of(Instant.now());
		boolean success = rrstore.saveRecording(recording);
		if (!success) {
			throw new Recording.RecordingSaveFailureException("Cannot delete recording");
		}
		return recording;
	}

    Optional<TemplateSet> getTemplateSet(String customerId, String app, String version);

    // void invalidateCacheFromTemplateSet(TemplateSet templateSet);

    void invalidateCache();

	boolean updateAgentConfigTag(AgentConfigTagInfo tagInfo);

	boolean saveAgentConfigAcknowledge(ConfigApplicationAcknowledge confApplicationAck);

	public void populateCache(CollectionKey collectionKey, RecordOrReplay rr);

	Pair<Result<ConfigApplicationAcknowledge> , List>getLatestAgentConfigAcknowledge(
		io.md.dao.CubeMetaInfo cubeMetaInfo, boolean facetOnNodeSelected, int forLastNsec);

	class ReqResp {


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
		RequestMatchTemplate,
		RequestCompareTemplate,
		ResponseCompareTemplate,
		ReplayStats,
        FuncReqResp,
        TemplateSet,
        TemplateUpdateOperationSet,
        GoldenSet,
        RecordingOperationSetMeta,
        RecordingOperationSet,
        MatchResultAggregate,
		Diff,
		AttributeTemplate,
		DynamicInjectionConfig,
		AgentConfigTagInfo,
		AgentConfig,
		AgentConfigAcknowledge;
	}

    /**
     * @param reqId
     * @return the matching request on the reqId
     */
    Optional<Event> getRequestEvent(String reqId);


    /**
	 *
	 * @param customerId
	 * @param app
	 * @param collection
	 * @param reqids
	 * @param services
	 * @param paths
	 * @param runType
	 * @return
	 */
	public Result<Event> getRequests(String customerId, String app, String collection,
		List<String> reqids, List<String> services, List<String> paths, Optional<Event.RunType> runType);

	@Override
    Result<Event> getEvents(EventQuery eventQuery);

    Optional<Event> getSingleResponseEvent(String customerId, String app, String collection, List<String> services,
		List<String> paths, Optional<Event.RunType> runType);


	/**
	 * @param replayId
	 * @return
	 */
	Optional<Replay> getReplay(String replayId);


	/**
	 * Save an analysis template for the given parameters in the backend
	 * database
	 * @param key
	 * @param templateAsJson
	 * @return
	 */
	String saveCompareTemplate(TemplateKey key, String templateAsJson)
		throws CompareTemplate.CompareTemplateStoreException;


	/**
	 * Save an app level attribute rule map (rules defined for attribute names in any api
	 * at any level for the app)
	 * @param key
	 * @param ruleMapJson
	 * @return
	 * @throws CompareTemplate.CompareTemplateStoreException
	 */
	public String saveAttributeRuleMap(TemplateKey key, String ruleMapJson)
		throws CompareTemplate.CompareTemplateStoreException;

	/**
	 * Retrieve an analysis template from the database for
	 * the given parameters
	 * @param key
	 * @return
	 */
	Optional<CompareTemplate> getCompareTemplate(TemplateKey key);

    Comparator getComparator(TemplateKey key, Optional<Event.EventType> eventType) throws
        TemplateNotFoundException;

    Comparator getComparator(TemplateKey key, Event.EventType eventType) throws
        TemplateNotFoundException;

    Comparator getComparator(TemplateKey key) throws TemplateNotFoundException;

    Optional<AttributeRuleMap> getAttributeRuleMap(TemplateKey key);


    /**
     *
     * @param customerId
     * @param app
     * @param instanceId
     * @param status
     * @param collection
     * @param numOfResults
     * @param start
     * @param userId
     * @param endDate
		 * @param startDate
		 * @Param testConfigName
		 * @Param goldenName
		 * @Param archived
     * @return
     */
    Result<Replay> getReplay(Optional<String> customerId, Optional<String> app, List<String> instanceId,
                             List<ReplayStatus> status, Optional<String> collection, Optional<Integer> numOfResults, Optional<Integer> start,
                             Optional<String> userId, Optional<Instant> endDate, Optional<Instant> startDate, Optional<String> testConfigName, Optional<String> goldenName, boolean archived);

    /**
     *
     * @param customerId
     * @param app
     * @param instanceId
     * @param status
     * @param collection
     * @param numOfResults
     * @return
     */
	Stream<Replay> getReplay(Optional<String> customerId, Optional<String> app, Optional<String> instanceId,
                             List<ReplayStatus> status, Optional<Integer> numOfResults, Optional<String> collection);

	/**
     * @param customerId
     * @param app
     * @param instanceId
     * @param status
     * @return
     */
    Stream<Replay> getReplay(Optional<String> customerId, Optional<String> app,
                             Optional<String> instanceId, ReplayStatus status);

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
     * @param resultAggregate
     * @return
     */
    boolean saveMatchResultAggregate(MatchResultAggregate resultAggregate);

	/**
	 * @param dynamicInjectionConfig
	 * @return
	 */
    String saveDynamicInjectionConfig(DynamicInjectionConfig dynamicInjectionConfig) throws SolrStoreException;


	/**
     * @param replayId
     * @param service
     * @return If service is empty, return aggregate results for all services. If
     * service is non-empty, return results for all paths in the service if bypath is true
     * This also returns the rollups (service, path), (service) () when service is empty.
     * and rollups (service, path) [bypath: true], (service)[bypath: false] when service is non-empty.
     * This method just gets the aggregates using Solr query which were pre-computed and
     * stored. For computation of aggregates check computeResultAggregate method.
     */
    Stream<MatchResultAggregate> getResultAggregate(String replayId, Optional<String> service,
                                                            boolean byPath);


    /**
	 * @param replayId
	 * @return
	 */
	Optional<Analysis> getAnalysis(String replayId);


	/**
	 * @param customerId
	 * @param app
	 * @param instanceId
	 * @param status
	 * @param collection
	 * @param templateVersion
	 * @param name
	 * @param parentRecordingId
	 * @param rootRecordingId
	 * @param codeVersion
	 * @param branch
	 * @param tags
	 * @param archived
	 * @param gitCommitId
	 * @param collectionUpdOpSetId
	 * @param templateUpdOpSetId
	 * @param userId
	 * @return
	 */
	Stream<Recording> getRecording(Optional<String> customerId, Optional<String> app, Optional<String> instanceId, Optional<RecordingStatus> status,
                                   Optional<String> collection, Optional<String> templateVersion, Optional<String> name, Optional<String> parentRecordingId, Optional<String> rootRecordingId,
                                   Optional<String> codeVersion, Optional<String> branch, List<String> tags, Optional<Boolean> archived, Optional<String> gitCommitId,
                                   Optional<String> collectionUpdOpSetId, Optional<String> templateUpdOpSetId, Optional<String> userId, Optional<String> label, Optional<String> recordingType,
																	 Optional<String> recordingId, Optional<Integer> numberOfResults, Optional<Integer> start);


    Optional<Recording> getRecording(String recordingId);


	/**
	 * @param customerId
	 * @param app
	 * @param instanceId
	 * @return
	 */
	Optional<String> getCurrentCollection(Optional<String> customerId, Optional<String> app,
			Optional<String> instanceId);


    /**
	 * @param customerId
	 * @param app
	 * @param instanceId
	 * @return For both record and replay, return the collection of the record stage
	 */
	Optional<String> getCurrentRecordingCollection(Optional<String> customerId, Optional<String> app,
			Optional<String> instanceId);


	/**
	 * @param recording
	 * @return
	 */
	boolean expireRecordingInCache(Recording recording);

	boolean forceDeleteInCache(Recording recording);

	boolean forceDeleteInCache(Replay replay);

	/**
	 * @param recording
	 * @return
	 */
	boolean saveRecording(Recording recording);

	/**
	 * @param customerId
	 * @param app
	 * @param collection
	 * @return
	 */
	Optional<Recording> getRecordingByCollectionAndTemplateVer(String customerId, String app, String collection,
                                                               Optional<String> templateSetVersion);

    /**
     * @param customerId
     * @param app
     * @param name
     * @param label
     * @return
     */
    Optional<Recording> getRecordingByName(String customerId, String app, String name, Optional<String> label);

	/**
	 * @param replayId
	 * @param service
	 * @return If service is empty, return aggregate results for all services. If
	 * service is non-empty, return results for all paths in the service if bypath is true
	 * This also returns the rollups (service, path), (service) ()
	 */
	Collection<MatchResultAggregate> computeResultAggregate(String replayId, Optional<String> service,
                                                            boolean bypath);

	/**
	 * @param customerId
	 * @param app
	 * @param instanceId
	 * @return
	 */
	Optional<RecordOrReplay> getCurrentRecordOrReplay(Optional<String> customerId, Optional<String> app,
                                                      Optional<String> instanceId);

	Optional<RecordOrReplay> getCurrentRecordOrReplay(Optional<String> customerId, Optional<String> app, Optional<String> instanceId, boolean extendTTL);
	/**
	 *
	 */
	boolean commit();


    /**
	 * Get ReqResponseMatchResult for the given request and replay Id
	 * @param recordReqId
	 * @param replayId
	 * @return
	 */
	Optional<ReqRespMatchResult> getAnalysisMatchResult(String recordReqId , String replayId);

    /**
     * Get ReqResponseMatchResult for the given replay Id, record req id and replay req id
     * It matches on both record and replay reqId. If any of them are empty, it requires the matching result to also
     * have empty value for that field
     * @param recordReqId
     * @param replayId
     * @return
     */
    Optional<ReqRespMatchResult> getAnalysisMatchResult(Optional<String> recordReqId, Optional<String> replayReqId,
                                                        String replayId);

	/**
	 * Get results matching a path and other constraints
	 * @param analysisMatchResultQuery
	 * @return
	 */
	ReqRespResultsWithFacets getAnalysisMatchResults(AnalysisMatchResultQuery analysisMatchResultQuery);


	/**
	 * Get ReqResponseMatchResult list for the given replay Id and filters out the results that has either Request or Response MatchType
	 * as NoMatch
	 * @param replayId
	 * @return
	 */
    Result<ReqRespMatchResult> getAnalysisMatchResultOnlyNoMatch(String replayId);

	/**
	 * Returns service facets with path sub-facets for each service
	 * @param collectionId
	 * @param runType
	 * @return
	 */
    ArrayList getServicePathHierarchicalFacets(String collectionId, RunType runType);

    /**
     * Deletes the Requests and Responses from the passed collection that has the given trace id
     * @param traceId
     * @param collectionName
     * @return
     */
    //Todo: Merge all these 3 delete apis in a single one which takes EventQuery
    //boolean deleteReqRes(EventQuery query);

    boolean deleteReqResByTraceId(String traceId, String collectionName);

    boolean deleteReqResByReqId(String reqId , String customerId, Optional<EventType> eventType);

    boolean deleteReqResByTraceId(String traceId , String customerId, String collection , Optional<EventType> eventType);



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

    boolean storeRecordingOperationSetMeta(RecordingOperationSetMeta recordingOperationSetMeta);

    // get recordingOperationSet for a given operationset id, service and path
    Optional<RecordingOperationSetMeta> getRecordingOperationSetMeta(String recordingOperationSetId);

    boolean storeRecordingOperationSet(RecordingOperationSetSP recordingOperationSetSP);

    // get recordingOperationSet for a given operationset id, service and path
    Optional<RecordingOperationSetSP> getRecordingOperationSetSP(String recordingOperationSetId, String service,
                                                                 String path);

    // get all recordingOperationSets for a given operationset id
    Stream<RecordingOperationSetSP> getRecordingOperationSetSPs(String recordingOperationSetId);

	/**
	 *
	 * @param apiTraceFacetQuery
	 * @return
	 */
    ArrayList getApiFacets(ApiTraceFacetQuery apiTraceFacetQuery);

	/**
	 *
	 * @param apiTraceFacetQuery
	 * @param addPathServiceFilter
     * @return
	 */
	Result<Event> getApiTrace(ApiTraceFacetQuery apiTraceFacetQuery, Optional<Integer> start, Optional<Integer> numberOfResults, List<EventType> eventTypes, boolean addPathServiceFilter);

    /**
     * Save a template set
     * @param templateSet Template Set
     * @return success flag
     */
	String saveTemplateSet(TemplateSet templateSet) throws Exception;

    Optional<TemplateSet> getTemplateSet(String templateSetId);

    Optional<TemplateSet> getLatestTemplateSet(String customer, String app);

    public void invalidateCurrentCollectionCache(String customerId, String app,
                                                 String instanceId);

    public Optional<String> getDefaultEventType(String customer, String app, String service
	    , String apiPath);

    boolean storeAgentConfig(ConfigDAO store);
    Optional<ConfigDAO> getAgentConfig(String customerId, String app, String service,
						String instanceId);

    Pair<List, Stream<ConfigDAO>> getAgentConfigWithFacets(String customerId, String app, Optional<String> service,
				Optional<String> instanceId, Optional<Integer> numOfResults, Optional<Integer> start, Optional<String> tag);
    Result<AgentConfigTagInfo> getAgentConfigTagInfoResults(String customerId, String app,
				Optional<String> service, String instanceId);

	boolean deleteAgentConfig(String customerId, String app, String service, String instanceId);

}
