/*
 *
 *    Copyright Cube I O
 *
 */

package io.md.services;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.md.constants.Constants;
import io.md.core.Comparator;
import io.md.core.Comparator.MatchType;
import io.md.dao.Event;
import io.md.dao.Event.EventType;
import io.md.dao.EventQuery;
import io.md.dao.MDTraceInfo;
import io.md.dao.RecordOrReplay;
import io.md.dao.ReqRespMatchResult;
import io.md.services.DataStore.TemplateNotFoundException;
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
    public MockResponse mock(Event reqEvent, Optional<Instant> lowerBoundForMatching) throws MockerException {
        Optional<String> recordingCollection = setPayloadKeyAndCollection(reqEvent);
        if (recordingCollection.isPresent()) {
            EventQuery eventQuery = buildRequestEventQuery(reqEvent, 0, 1, true, lowerBoundForMatching, recordingCollection.get());
            DSResult<Event> res = cube.getEvents(eventQuery);
            Optional<Event> matchingResponse = res.getObjects().findFirst()
                .flatMap(cube::getRespEventForReqEvent);

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
            createResponseFromEvent(reqEvent, matchingResponse);
            return new MockResponse(matchingResponse, res.getNumFound());
        } else {
            String errorReason = "Invalid event or no record/replay found.";
            LOGGER.error(createMockReqErrorLogMessage(reqEvent, errorReason));
            throw new MockerException(Constants.INVALID_EVENT,
                errorReason);
        }
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

    private static EventQuery buildRequestEventQuery(Event event, int offset, int limit,
        boolean isSortOrderAsc, Optional<Instant> lowerBoundForMatching, String collection) {
        EventQuery.Builder builder =
        new EventQuery.Builder(event.customerId, event.app, event.eventType)
            .withService(event.service)
            .withCollection(collection)
            //.withInstanceId(event.instanceId)
            .withPaths(Arrays.asList(event.apiPath))
            .withTraceId(event.getTraceId())
            .withPayloadKey(event.payloadKey)
            .withOffset(offset)
            .withLimit(limit)
            .withSortOrderAsc(isSortOrderAsc);
        lowerBoundForMatching.ifPresent(builder::withTimestamp);
        return builder.build();
    }

    private Optional<String> setPayloadKeyAndCollection(Event event) {
        Optional<String> ret = Optional.empty();
        Optional<RecordOrReplay> recordOrReplay = cube.getCurrentRecordOrReplay(
            event.customerId, event.app, event.instanceId);
        Optional<String> replayCollection = recordOrReplay.flatMap(RecordOrReplay::getCollection);
        Optional<String> collection = recordOrReplay
            .flatMap(RecordOrReplay::getRecordingCollection);
        // check collection, validate, fetch template for request, set key and store. If error at any point stop
        if (collection.isPresent() && replayCollection.isPresent()) {
            event.setCollection(replayCollection.get());
            try {
                event.parseAndSetKey(cube
                    .getRequestMatchTemplate(event, recordOrReplay.get().getTemplateVersion()));
            } catch (TemplateNotFoundException e) {
                LOGGER.error(Utils.createLogMessasge(
                    "message", "Compare template not found.",
                    "type", event.eventType,
                    "reqId", event.reqId,
                    "path", event.apiPath), e);
            }
            ret = collection;
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
        return ret;
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

    private void createResponseFromEvent(
        Event mockRequestEvent, Optional<Event> respEvent) {

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
            respEvent.ifPresent(respEventVal -> {
                try {
                    Event mockResponseToStore = createMockResponseEvent(respEventVal,
                        Optional.of(mockRequestEvent.reqId),
                        mockRequestEvent.instanceId, mockRequestEvent.getCollection());
                    cube.save(mockResponseToStore);
                } catch (Event.EventBuilder.InvalidEventException e) {
                    LOGGER.error(Utils.createLogMessasge(
                        Constants.MESSAGE, "Not able to store mock response event",
                        Constants.TRACE_ID_FIELD, respEventVal.getTraceId(),
                        Constants.REQ_ID_FIELD, respEventVal.reqId));
                }
            });
        }
        return;
    }

    /**
     * Create a dummy response event (just for the records) to save against the dummy mock request
     * @param originalResponse
     * @param mockReqId
     * @param instanceId
     * @param replayCollection
     * @return
     */
    static private Event createMockResponseEvent(Event originalResponse,
                                          Optional<String> mockReqId,
                                          String instanceId,
                                          String replayCollection) throws Event.EventBuilder.InvalidEventException {
        Event.EventBuilder builder = new Event.EventBuilder(originalResponse.customerId, originalResponse.app,
            originalResponse.service,
            instanceId, replayCollection,
            new MDTraceInfo(originalResponse.getTraceId() , null, null),
            Event.RunType.Replay, Optional.of(Instant.now()),
            mockReqId.orElse("NA"),
            originalResponse.apiPath, Event.EventType.HTTPResponse, originalResponse.recordingType);
        return builder.setPayload(originalResponse.payload).createEvent();
    }

}
