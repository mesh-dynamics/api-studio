/*
 *
 *    Copyright Cube I O
 *
 */

package io.md.services;

import io.md.cache.ProtoDescriptorCache;
import io.md.cache.ProtoDescriptorCache.ProtoDescriptorKey;
import io.md.core.CollectionKey;
import io.md.dao.*;

import io.md.injection.DynamicInjectionConfig;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import io.md.logger.LogMgr;
import org.apache.http.HttpStatus;
import io.md.injection.DynamicInjector;
import io.md.injection.DynamicInjectorFactory;
import io.md.utils.CubeObjectMapperProvider;
import org.slf4j.Logger;

import io.md.constants.Constants;
import io.md.core.Comparator;
import io.md.core.Comparator.MatchType;
import io.md.core.TemplateKey.Type;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.Event.EventType;
import io.md.services.DataStore.TemplateNotFoundException;
import io.md.utils.ProtoDescriptorCacheProvider;
import io.md.utils.UtilException;
import io.md.utils.Utils;

import static io.md.dao.Event.RunType.*;

import com.fasterxml.jackson.databind.node.TextNode;


/*
 * Created by IntelliJ IDEA.
 * Date: 14/05/20
 */
public class RealMocker implements Mocker {

    private final  DataStore cube;
    private final DynamicInjectorFactory diFactory;
    private static final List<Event.RunType> nonMockRunTypes = Arrays.stream(Event.RunType.values()).filter(rt->rt!=Mock).collect(Collectors.toList());

    private static final Logger LOGGER = LogMgr.getLogger(RealMocker.class);

    public RealMocker(DataStore cube) {
        this.cube = cube;
        this.diFactory = new DynamicInjectorFactory(cube , CubeObjectMapperProvider.getInstance());
    }

    @Override
    public MockResponse mock(Event reqEvent, Optional<Instant> lowerBoundForMatching, Optional<MockWithCollection> mockWithCollections) throws MockerException {
        Optional<MockWithCollection> mockWithCollection = setPayloadKeyAndCollection(reqEvent, mockWithCollections);
        if (mockWithCollection.isPresent()) {
            MockWithCollection mockWColl = mockWithCollection.get();
            DynamicInjector di = diFactory.getMgr(reqEvent.customerId , reqEvent.app , mockWColl.dynamicInjectionConfigVersion);
            DynamicInjector si = diFactory.getMgr(reqEvent.customerId, reqEvent.app,
                mockWColl.dynamicInjectionConfigVersion
                    .map(config -> config + DynamicInjectionConfig.staticVersionSuffix));
            di.extract(reqEvent , null);

            List<String> payloadFieldFilterList = mockWColl.isDevtool ? reqEvent.payloadFields: Collections.EMPTY_LIST;

            Optional<JoinQuery> joinQuery = mockWColl.isDevtool ? Optional.of(getSuccessResponseMatch(reqEvent)) : Optional.empty();
            Optional<ReplayContext> replayCtx = mockWColl.replay.flatMap(r->r.replayContext);

            boolean tracePropagation = mockWColl.replay.map(r->r.tracePropagation).orElse(true);
            //For devtool get the latest success match - isTimestampSortOrderAsc = false
            //For regression -> get the first match - isTimestampSortOrderAsc = true
            EventQuery eventQuery = buildRequestEventQuery(reqEvent, 0, Optional.of(1),
                !mockWColl.isDevtool, lowerBoundForMatching, mockWColl.recordCollection,
                payloadFieldFilterList , joinQuery , mockWColl.isDevtool , replayCtx , tracePropagation);
            DSResult<Event> res = cube.getEvents(eventQuery);

            final Map<String , Event> reqIdReqMapping = new HashMap<>();
            //saving the request and requestId mapping
            Function<Event , Optional<Event>> getRespEventForReqEvent = (req)->{
                reqIdReqMapping.put(req.reqId , req);
                return cube.getRespEventForReqEvent(req);
            };

            Optional<Event> matchingRequest = res.getObjects().findFirst();
            Optional<Event> matchingResponse = matchingRequest.flatMap(getRespEventForReqEvent);
            if(!mockWColl.isDevtool && matchingRequest.isPresent() && res.getNumFound()>1){
                ReplayContext replyCtx = replayCtx.orElse(new ReplayContext());
                replyCtx.setMockResultToReplayContext(matchingRequest.get());
                mockWColl.replay.ifPresent(replay->{
                    replay.replayContext = Optional.of(replyCtx);
                    cube.populateCache(new CollectionKey(replay.customerId, replay.app , replay.instanceId) , RecordOrReplay.createFromReplay(replay));
                });
            }
            
            if(mockWColl.isDevtool && !matchingResponse.isPresent()){
                LOGGER.info(createMockReqErrorLogMessage(reqEvent,
                        "Did not find any valid success response. Giving first match resp"));
                //If there is no success match then get the first latest match.
                //JoinQuery - Empty
                eventQuery = buildRequestEventQuery(reqEvent, 0, Optional.of(1),
                    false , lowerBoundForMatching, mockWColl.recordCollection ,
                    payloadFieldFilterList , Optional.empty() , mockWColl.isDevtool , Optional.empty() , true);
                res = cube.getEvents(eventQuery);
                matchingResponse = res.getObjects().findFirst().flatMap(getRespEventForReqEvent);
            }

            if (!matchingResponse.isPresent()) {
                LOGGER.info(createMockReqErrorLogMessage(reqEvent,
                    "Using default response(as no matching request event found)"));
                EventQuery defaultEventQuery = buildDefaultRespEventQuery(reqEvent);
                res = cube.getEvents(defaultEventQuery);
                matchingResponse = res.getObjects().findFirst();
                if (!matchingResponse.isPresent()) {
                    LOGGER.error(createMockReqErrorLogMessage(reqEvent,
                        "Unable to mock request since no default response found"));
                }
            }
            matchingResponse.ifPresent(di::inject);
            matchingResponse.ifPresent(si::inject);

            Optional<Event> matchedReq = matchingResponse.map(resp->reqIdReqMapping.get(resp.reqId));
            Optional<Event> mockResponse = createResponseFromEvent(reqEvent, matchedReq , matchingResponse, mockWithCollection.get().runId, mockWColl.isDevtool);
            return new MockResponse(mockResponse, res.getNumFound());
        } else {
            String errorReason = "Invalid event or no record/replay found.";
            LOGGER.error(createMockReqErrorLogMessage(reqEvent, errorReason));
            throw new MockerException(Constants.INVALID_EVENT,
                errorReason);
        }
    }

    private JoinQuery getSuccessResponseMatch(Event reqEvent){

        JoinQuery.Builder builder = new JoinQuery.Builder();
        Map<String,String> successfulRespCond = new HashMap<>();
        successfulRespCond.put(Constants.EVENT_TYPE_FIELD , EventType.getResponseType(reqEvent.eventType).toString());
        successfulRespCond.put(Constants.PAYLOAD_FIELDS_FIELD , String.format("%s:%s", Constants.STATUS_PATH, String.valueOf(reqEvent.payload instanceof GRPCPayload ? Constants.GRPC_SUCCESS_STATUS_CODE : HttpStatus.SC_OK)));

        builder.withAndConds(successfulRespCond);

        return builder.build();

    }

    private boolean isSuccessResponse(Event respEvent){
        //Payload present
        if(respEvent.payload==null) return false;
        //Allow all Non http Response payload
        if(!(respEvent.payload instanceof HTTPResponsePayload)) return true;

        HTTPResponsePayload httpRespPayload = (HTTPResponsePayload) respEvent.payload;
        int status = httpRespPayload.getStatus();
        // All 2xx & 3xx OK
        return (status >= 200 && status < 400);
    }

    private String createMockReqErrorLogMessage(Event reqEvent, String errStr) {
        return Utils.createLogMessasge(
            Constants.MESSAGE, errStr,
            Constants.CUSTOMER_ID_FIELD, reqEvent.customerId,
            Constants.APP_FIELD, reqEvent.app,
            Constants.INSTANCE_ID_FIELD, reqEvent.instanceId,
            Constants.SERVICE_FIELD, reqEvent.service,
            Constants.PATH_FIELD, reqEvent.apiPath,
            Constants.TRACE_ID_FIELD, reqEvent.getTraceId(),
            Constants.REPLAY_ID_FIELD, reqEvent.getCollection());
    }

    private EventQuery buildRequestEventQuery(Event event, int offset, Optional<Integer> limit,
        boolean isTimestampSortOrderAsc, Optional<Instant> lowerBoundForMatching, String collection ,
        List<String> payloadFields , Optional<JoinQuery> joinQuery , boolean isDevtoolRequest , Optional<ReplayContext> replayContext , boolean tracePropagation) {
        EventQuery.Builder builder =
            new EventQuery.Builder(event.customerId, event.app, event.eventType)
                .withService(event.service)
                .withCollection(collection , isDevtoolRequest ? EventQuery.COLLECTION_WEIGHT : null)
                //.withInstanceId(event.instanceId)
                .withPaths(Arrays.asList(event.apiPath))
                .withTraceId(event.getTraceId() , (isDevtoolRequest || !tracePropagation) ? EventQuery.TRACEID_WEIGHT : null)
                .withPayloadKey(event.payloadKey , isDevtoolRequest ? EventQuery.PAYLOAD_KEY_WEIGHT : null)
                .withOffset(offset)
                .withTimestampAsc(isTimestampSortOrderAsc)
                .withPayloadFields(payloadFields)
                .withRunTypes(nonMockRunTypes);
        if(replayContext.isPresent()){
            ReplayContext rCtx = replayContext.get();
            Optional<Instant> reqStartTs = Optional.ofNullable(rCtx.getLastMockEventTs(event).orElse(rCtx.reqStartTs.orElse(null)));
            reqStartTs.ifPresent(builder::withStartTimestamp);
            rCtx.reqEndTs.ifPresent(builder::withEndTimestamp);
        }else{
            lowerBoundForMatching.ifPresent(builder::withStartTimestamp);
        }

        limit.ifPresent(builder::withLimit);
        joinQuery.ifPresent(builder::withJoinQuery);

        return builder.build();
    }

    private Optional<MockWithCollection> setPayloadKeyAndCollection(Event event, Optional<MockWithCollection> mockCtx) {

        if(!mockCtx.isPresent()){
            mockCtx = Utils.getMockCollection(cube , event.customerId, event.app, event.instanceId, false );
            if(!mockCtx.isPresent()){
                LOGGER
                    .error(Utils.createLogMessasge(
                        Constants.REASON, "Collection not found",
                        Constants.CUSTOMER_ID_FIELD, event.customerId,
                        Constants.APP_FIELD, event.app,
                        Constants.INSTANCE_ID_FIELD, event.instanceId,
                        Constants.TRACE_ID_FIELD, event.getTraceId()));
            }
        }

        mockCtx.ifPresent(ctx->{
            String replayCollection = ctx.replayCollection;
            String templateVersion = ctx.templateVersion;
            String runId = ctx.runId;
            Optional<ReplayContext> replayCtx = ctx.replay.flatMap(r->r.replayContext);

            event.setCollection(replayCollection);
            replayCtx.flatMap(rctx->rctx.reqTraceId).ifPresent(event::setTraceId);

            try {
                if(event.payload instanceof GRPCPayload) {
                    ProtoDescriptorCache protoDescriptorCache = ProtoDescriptorCacheProvider
                        .getInstance()
                        .get();
                    GRPCPayload grpcPayload = ((GRPCPayload) event.payload);
                    Optional<ProtoDescriptorDAO> protoDescriptorDAO =
                        protoDescriptorCache.get(new ProtoDescriptorKey(event.customerId, event.app, event.getCollection()));
                    grpcPayload.setProtoDescriptor(protoDescriptorDAO);
                    try {
                        grpcPayload.dataObj.put(Constants.METHOD_PATH,
                            new JsonDataObj(new TextNode("POST"), CubeObjectMapperProvider.getInstance()));
                        // Need to add path field in dataObj otherwise will error out while deserialisng
                        grpcPayload.dataObj.put(Constants.PATH_PATH,
                            new JsonDataObj(new TextNode(event.apiPath), CubeObjectMapperProvider.getInstance()));
                    } catch (Exception e) {
                        LOGGER.error("Unable to set method as post in GRPCRequestPayload dataobj", e);
                    }
                }
                event.parseAndSetKey(cube.getTemplate(event.customerId, event.app, event.service,
                    event.apiPath, templateVersion, Type.RequestMatch
                    , Optional.ofNullable(event.eventType), Utils.extractMethod(event), replayCollection));
                event.setRunId(runId);
            } catch (TemplateNotFoundException e) {
                LOGGER.error(Utils.createLogMessasge(
                    "message", "Compare template not found.",
                    "type", event.eventType,
                    "reqId", event.reqId,
                    "path", event.apiPath), e);
            }
        });

        if (shouldStore(event.eventType)) {
            cube.save(event);
        }

        return mockCtx;
    }

    private boolean shouldStore(EventType eventType) {
        // returns true for events that we want to record during mocking
        // currently only HTTP requests since it will be too much overhead for JavaRequests
        return eventType == EventType.HTTPRequest || eventType == EventType.HTTPResponse;
    }


    static private EventQuery buildDefaultRespEventQuery(Event mockRequest) {
        EventQuery.Builder eventQuery = new EventQuery.Builder(
            mockRequest.customerId,
            mockRequest.app, EventType.getResponseType(mockRequest.eventType));
        return eventQuery
            .withService(mockRequest.service)
            .withPaths(Arrays.asList(mockRequest.apiPath))
            .withRunType(Manual)
            .build();
    }

    private Optional<Event> createResponseFromEvent(
        Event mockRequestEvent, Optional<Event> matchedReq ,  Optional<Event> respEvent, String runId , boolean isDevtoolRequest) {

        Optional<Event> mockResponse = respEvent;

        if (shouldStore(mockRequestEvent.eventType)) {

            boolean collectionMatched = matchedReq.map(mr->mr.getCollection().equals(mockRequestEvent.getCollection())).orElse(false);
            boolean traceIdMatched    = matchedReq.map(mr->mr.getTraceId().equals(mockRequestEvent.getTraceId())).orElse(false);
            boolean payloadKeyMatched    = matchedReq.map(mr->mr.payloadKey == mockRequestEvent.payloadKey).orElse(false);

            //Optional<String> score  = matchedReq.flatMap(e->e.getMetaFieldValue(Constants.SCORE_FIELD));
            MatchType reqMatch = matchedReq.map(val->{
                return isDevtoolRequest ? (collectionMatched && traceIdMatched && payloadKeyMatched ? MatchType.ExactMatch : MatchType.FuzzyMatch) : MatchType.ExactMatch ;
            }).orElse(MatchType.NoMatch);

            Map<String,String> meta = new HashMap<>();
            meta.put(Constants.MATCH_TYPE , reqMatch.toString());
            matchedReq.ifPresent(req->{
                meta.put(Constants.MATCHED_REQUEST_ID , req.reqId);
                meta.put(Constants.MATCHED_COLLECTION_NAME , req.getCollection());
                meta.put(Constants.COLLECTION_MATCHED , Boolean.toString(collectionMatched));
                meta.put(Constants.TRACEID_MATCHED , Boolean.toString(traceIdMatched));
                meta.put(Constants.PAYLOAD_KEY_MATCHED , Boolean.toString(payloadKeyMatched));
            });

            respEvent.ifPresent(e->meta.put(Constants.MATCHED_RESPONSE_ID , e.reqId));

            // store a req-resp analysis match result for the mock request (during replay)
            // and the matched recording request
            MatchType respMatch = respEvent.map(e -> MatchType.ExactMatch)
                .orElse(MatchType.Default);
            ReqRespMatchResult matchResult =
                new ReqRespMatchResult(respEvent.map(Event::getReqId)
                    , Optional.ofNullable(mockRequestEvent.reqId), reqMatch
                    , 1, mockRequestEvent.getCollection(), mockRequestEvent.service
                    , mockRequestEvent.apiPath, respEvent.map(Event::getTraceId)
                    , Optional.of(mockRequestEvent.getTraceId()), respEvent.map(Event::getSpanId),
                    respEvent.map(Event::getParentSpanId),
                    Optional.ofNullable(mockRequestEvent.spanId),
                    Optional.ofNullable(mockRequestEvent.parentSpanId),
                    new Comparator.Match(respMatch, "", Collections.emptyList()),
                    new Comparator.Match(
                        respMatch, "", Collections.emptyList()));

            cube.saveResult(matchResult, mockRequestEvent.customerId);
            try {
                mockResponse = createMockResponseEvent(mockRequestEvent, respEvent,
                    Optional.of(mockRequestEvent.reqId),
                    mockRequestEvent.instanceId, mockRequestEvent.getCollection(), runId, meta);
                mockResponse.ifPresent(cube::save);
            } catch (InvalidEventException e) {
                LOGGER.error(Utils.createLogMessasge(
                    Constants.MESSAGE, "Not able to store mock response event",
                    Constants.TRACE_ID_FIELD, mockRequestEvent.getTraceId(),
                    Constants.REQ_ID_FIELD, mockRequestEvent.reqId));
            }
        }
        return mockResponse;
    }

    /**
     * Create a dummy response event (just for the records) to save against the dummy mock request
     * @param originalResponse
     * @param mockReqId
     * @param instanceId
     * @param replayCollection
     * @return
     */
    private Optional<Event> createMockResponseEvent(Event mockRequest, Optional<Event> originalResponse,
        Optional<String> mockReqId,
        String instanceId,
        String replayCollection, String runId , Map<String , String> meta)
        throws Event.EventBuilder.InvalidEventException {
        Event.EventBuilder builder = new Event.EventBuilder(mockRequest.customerId, mockRequest.app,
            mockRequest.service,
            instanceId, replayCollection,
            new MDTraceInfo(mockRequest.getTraceId() , null, null),
            mockRequest.getRunType(), Optional.of(mockRequest.timestamp),
            mockReqId.orElse("NA"),
            mockRequest.apiPath, EventType.getResponseType(mockRequest.eventType), mockRequest.recordingType)
                .withRunId(runId)
                .withMetaData(meta);
        Optional<Payload> payload = originalResponse.map(event -> event.payload);

        if (!payload.isPresent()) {
            payload = createNoMatchResponsePayload(mockRequest);
        }
        return payload.map(UtilException.rethrowFunction(p -> builder.setPayload(p).createEvent()));
    }

    private Optional<Payload> createNoMatchResponsePayload(Event reqEvent) {
        if (reqEvent.eventType.equals(EventType.HTTPRequest)) {
            return Optional.of(new HTTPResponsePayload(emptyMVMap, Status.NOT_FOUND.getStatusCode(), emptyBody));
        } else {
            String errorReason = "Empty response not implemented for " + reqEvent.eventType.toString();
            LOGGER.error(createMockReqErrorLogMessage(reqEvent, errorReason));
            return Optional.empty();
        }

    }

    static private final MultivaluedMap<String, String> emptyMVMap = new MultivaluedHashMap<>();
    static private final byte[] emptyBody = new byte[0];
}