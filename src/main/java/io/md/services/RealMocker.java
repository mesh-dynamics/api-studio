/*
 *
 *    Copyright Cube I O
 *
 */

package io.md.services;

import io.md.dao.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.md.constants.Constants;
import io.md.core.Comparator;
import io.md.core.Comparator.MatchType;
import io.md.core.TemplateKey.Type;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.Event.EventType;
import io.md.services.DataStore.TemplateNotFoundException;
import io.md.utils.UtilException;
import io.md.utils.Utils;


/*
 * Created by IntelliJ IDEA.
 * Date: 14/05/20
 */
public class RealMocker implements Mocker {

    private DataStore cube;
    private static final Logger LOGGER = LoggerFactory.getLogger(RealMocker.class);

    public RealMocker(DataStore cube) {
        this.cube = cube;
    }

    @Override
    public MockResponse mock(Event reqEvent, Optional<Instant> lowerBoundForMatching, Optional<MockWithCollection> mockWithCollections) throws MockerException {
        Optional<MockWithCollection> mockWithCollection = setPayloadKeyAndCollection(reqEvent, mockWithCollections);
        if (mockWithCollection.isPresent()) {
            MockWithCollection mockWColl = mockWithCollection.get();
            // devtool sortOrder -> desc , asc otherwise for normal mock
            boolean isSortOrderAsc = !mockWColl.isDevtool;
            //devtool let all results come. No limit
            Integer limit = mockWColl.isDevtool ? null : 1;
            EventQuery eventQuery = buildRequestEventQuery(reqEvent, 0, limit, isSortOrderAsc, lowerBoundForMatching, mockWithCollection.get().recordCollection);
            DSResult<Event> res = cube.getEvents(eventQuery);

            Optional<Event> matchingResponse = mockWColl.isDevtool==false ?
                    //Normal Replay Mock - Old logic of getting first event and getting response corresponding to it
                    res.getObjects().findFirst().flatMap(cube::getRespEventForReqEvent) :
                    // Devtool Mock -> Find the response for each matched request un-till success response is found
                    res.getObjects().map(cube::getRespEventForReqEvent)
                    .filter(this::isSuccessResponse)
                    .map(Optional::get)
                    .findFirst();


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
            Optional<Event> mockResponse = createResponseFromEvent(reqEvent, matchingResponse, mockWithCollection.get().runId);
            return new MockResponse(mockResponse, res.getNumFound());
        } else {
            String errorReason = "Invalid event or no record/replay found.";
            LOGGER.error(createMockReqErrorLogMessage(reqEvent, errorReason));
            throw new MockerException(Constants.INVALID_EVENT,
                errorReason);
        }
    }

    private boolean isSuccessResponse(Optional<Event> response){
        //Ignore Absent Response
        if(!response.isPresent()) return false;

        Event respEvent = response.get();
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

    private static EventQuery buildRequestEventQuery(Event event, int offset, Integer limit,
        boolean isSortOrderAsc, Optional<Instant> lowerBoundForMatching, String collection) {
        EventQuery.Builder builder =
            new EventQuery.Builder(event.customerId, event.app, event.eventType)
                .withService(event.service)
                .withCollection(collection , Constants.COLLECTION_WEIGHT)
                //.withInstanceId(event.instanceId)
                .withPaths(Arrays.asList(event.apiPath))
                .withTraceId(event.getTraceId() , Constants.TRACEID_WEIGHT)
                .withPayloadKey(event.payloadKey , Constants.PAYLOAD_KEY_WEIGHT)
                .withOffset(offset)
                .withLimit(limit)
                .withSortOrderAsc(isSortOrderAsc);
        lowerBoundForMatching.ifPresent(builder::withTimestamp);
        return builder.build();
    }

    private Optional<MockWithCollection> setPayloadKeyAndCollection(Event event, Optional<MockWithCollection> mockWithCollections) {

        MockWithCollection mockWithCollection = mockWithCollections.orElseGet(()->Utils.getMockCollection(cube , event.customerId, event.app, event.instanceId, false ));

        Optional<String> replayCollection = Optional.of(mockWithCollection.replayCollection);
        Optional<String> collection = Optional.of(mockWithCollection.recordCollection);
        Optional<String> templateVersion = Optional.of(mockWithCollection.templateVersion);
        Optional<String> optionalRunId = Optional.of(mockWithCollection.runId);

        // check collection, validate, fetch template for request, set key and store. If error at any point stop
        if (collection.isPresent() && replayCollection.isPresent() && templateVersion.isPresent()) {
            String runId = optionalRunId.orElse(event.getTraceId());
            mockWithCollection.runId = runId;
            event.setCollection(replayCollection.get());
            try {
                event.parseAndSetKey(cube.getTemplate(event.customerId, event.app, event.service,
                    event.apiPath, templateVersion.get(), Type.RequestMatch
                    , Optional.ofNullable(event.eventType), Utils.extractMethod(event), replayCollection.get()));
                event.setRunId(runId);
            } catch (TemplateNotFoundException e) {
                LOGGER.error(Utils.createLogMessasge(
                    "message", "Compare template not found.",
                    "type", event.eventType,
                    "reqId", event.reqId,
                    "path", event.apiPath), e);
            }
        } else {
            LOGGER
                .error(Utils.createLogMessasge(
                    Constants.REASON, "Collection not found",
                    Constants.CUSTOMER_ID_FIELD, event.customerId,
                    Constants.APP_FIELD, event.app,
                    Constants.INSTANCE_ID_FIELD, event.instanceId,
                    Constants.TRACE_ID_FIELD, event.getTraceId()));
        }
        if (shouldStore(event.eventType)) {
            cube.save(event);
        }
        return Optional.of(mockWithCollection);
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
            .withRunType(Event.RunType.Manual)
            .build();
    }

    private Optional<Event> createResponseFromEvent(
        Event mockRequestEvent, Optional<Event> respEvent, String runId) {

        Optional<Event> mockResponse = respEvent;

        if (shouldStore(mockRequestEvent.eventType)) {

            // store a req-resp analysis match result for the mock request (during replay)
            // and the matched recording request
            MatchType reqMatch = respEvent.map(e -> MatchType.ExactMatch).orElse(MatchType.NoMatch);
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

            cube.saveResult(matchResult);
            try {
                mockResponse = createMockResponseEvent(mockRequestEvent, respEvent,
                    Optional.of(mockRequestEvent.reqId),
                    mockRequestEvent.instanceId, mockRequestEvent.getCollection(), runId);
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
        String replayCollection, String runId)
        throws Event.EventBuilder.InvalidEventException {
        Event.EventBuilder builder = new Event.EventBuilder(mockRequest.customerId, mockRequest.app,
            mockRequest.service,
            instanceId, replayCollection,
            new MDTraceInfo(mockRequest.getTraceId() , null, null),
            Event.RunType.Replay, Optional.of(Instant.now()),
            mockReqId.orElse("NA"),
            mockRequest.apiPath, EventType.getResponseType(mockRequest.eventType), mockRequest.recordingType).withRunId(runId);
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