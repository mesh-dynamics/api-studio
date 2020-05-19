/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.services;

import static io.md.dao.FnReqRespPayload.RetStatus.Success;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.cube.agent.FnResponse;
import io.md.core.Comparator;
import io.md.dao.DataObj;
import io.md.dao.Event;
import io.md.dao.EventQuery;
import io.md.dao.FnReqRespPayload;
import io.md.dao.MDTraceInfo;
import io.md.utils.UtilException;

import com.cube.cache.ComparatorCache;
import com.cube.cache.TemplateKey;
import com.cube.core.Utils;
import com.cube.dao.ReqRespMatchResult;
import com.cube.dao.ReqRespStore;
import com.cube.utils.Constants;

/*
 * Created by IntelliJ IDEA.
 * Date: 14/05/20
 */
public class RealMocker implements Mocker {

    private DataStore cube;
    private static final Logger LOGGER = LogManager.getLogger(RealMocker.class);

    public RealMocker(DataStore cube) {
        this.cube = cube;
    }

    @Override
    public FnResponse mockFunction(Event event) throws MockerException {
        if (setFunctionPayloadKeyAndCollection(event)) {
            EventQuery eventQuery = buildFunctionEventQuery(event, 0, 1, true);
            DSResult<Event> matchingEvent = cube.getEvents(eventQuery);

            return matchingEvent.getObjects().findFirst()
                .map(UtilException.rethrowFunction(retEvent -> getFuncResp(event, matchingEvent.getNumFound(),
                    retEvent)))
                .orElseGet(UtilException.rethrowSupplier(() -> getDefaultFuncResp(event)));
        } else {
            String errorReason = "Invalid event or no record/replay found.";
            LOGGER.error(new ObjectMessage(
                Map.of(
                    Constants.API_PATH_FIELD, event.apiPath,
                    Constants.TRACE_ID_FIELD, event.getTraceId(),
                    Constants.REASON, errorReason
                )));
            throw new MockerException(Constants.INVALID_EVENT,
                errorReason);
        }
    }

    @Override
    public Optional<Event> getResp(MultivaluedMap<String, String> queryParams, String path,
                                   MultivaluedMap<String, String> formParams,
                                   String customerId, String app, String instanceId,
                                   String service, String method, String body,
                                   MultivaluedMap<String, String> headers) {

        LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Attempting to mock request",
            Constants.CUSTOMER_ID_FIELD, customerId, Constants.APP_FIELD, app
            , Constants.INSTANCE_ID_FIELD, instanceId, Constants.SERVICE_FIELD, service,
            Constants.METHOD_FIELD, method, Constants.PATH_FIELD, path)));

        // pathParams are not used in our case, since we are matching full path
        // MultivaluedMap<String, String> pathParams = ui.getPathParameters();
        Optional<ReqRespStore.RecordOrReplay> recordOrReplay = cube
            .getCurrentRecordOrReplay(customerId, app, instanceId);
        Optional<String> collectionOpt = recordOrReplay
            .flatMap(ReqRespStore.RecordOrReplay::getRecordingCollection);
        Optional<String> replayIdOpt = recordOrReplay
            .flatMap(ReqRespStore.RecordOrReplay::getCollection);
        /* TODO: looks like we are not using this any more -- can be removed once we are sure
        boolean considerTrace = Utils.strToBool(headers.getRequestHeaders()
            .getFirst("cube-consider-trace")).orElse(true);
            */

        if (replayIdOpt.isEmpty() || collectionOpt.isEmpty()) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Unable to mock request since replay/collection is empty",
                    Constants.CUSTOMER_ID_FIELD, customerId, Constants.APP_FIELD, app
                    , Constants.INSTANCE_ID_FIELD, instanceId, Constants.SERVICE_FIELD, service,
                    Constants.METHOD_FIELD, method, Constants.PATH_FIELD, path, Constants.BODY,
                    body)));
            return Optional.empty();
        }

        String replayId = replayIdOpt.get();
        String collection = collectionOpt.get();

        String templateVersion = recordOrReplay.get().getTemplateVersion();

        TemplateKey key = new TemplateKey(templateVersion, customerId, app, service, path, TemplateKey.Type.RequestMatch);
        Comparator comparator = null;
        try {
            comparator = cube.getComparator(key , Event.EventType.HTTPRequest);
        } catch (ComparatorCache.TemplateNotFoundException e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Unable to mock request since request comparator not found",
                    Constants.CUSTOMER_ID_FIELD, customerId, Constants.APP_FIELD, app
                    , Constants.INSTANCE_ID_FIELD, instanceId, Constants.SERVICE_FIELD, service,
                    Constants.METHOD_FIELD, method, Constants.PATH_FIELD, path, Constants.BODY,
                    body, Constants.REPLAY_ID_FIELD , replayId)), e);
            return Optional.empty();
        }

        // first store the original request as a part of the replay
        Event mockRequestEvent;
        try {
            mockRequestEvent = createRequestMockNew(path, formParams, customerId, app, instanceId,
                service, method, body, headers, queryParams, replayId, comparator);
            cube.save(mockRequestEvent);
        } catch (Exception e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Unable to mock request, exception while creating request",
                    Constants.CUSTOMER_ID_FIELD, customerId, Constants.APP_FIELD, app
                    , Constants.INSTANCE_ID_FIELD, instanceId, Constants.SERVICE_FIELD, service,
                    Constants.METHOD_FIELD, method, Constants.PATH_FIELD, path, Constants.BODY,
                    body, Constants.REPLAY_ID_FIELD , replayId)), e);
            return Optional.empty();
        }

        EventQuery reqQuery = getRequestEventQuery(mockRequestEvent, collection, 1 /*, considerTrace */);
        Optional<Event> respEvent = cube.getSingleEvent(reqQuery)
            .flatMap(event -> cube.getRespEventForReqEvent(event))
            .or(() -> {
                LOGGER.info(new ObjectMessage(
                    Map.of(Constants.MESSAGE, "Using default response(as no matching request event found)",
                        Constants.CUSTOMER_ID_FIELD, customerId, Constants.APP_FIELD, app
                        , Constants.INSTANCE_ID_FIELD, instanceId, Constants.SERVICE_FIELD, service,
                        Constants.METHOD_FIELD, method, Constants.PATH_FIELD, path, Constants.TRACE_ID_FIELD,
                        String.join(":", reqQuery.getTraceIds()) , Constants.REPLAY_ID_FIELD , replayId)));

                EventQuery respQuery = getDefaultRespEventQuery(mockRequestEvent);
                Optional<Event> defRespEvent = cube.getSingleEvent(respQuery);
                if (defRespEvent.isPresent()) {
                    return defRespEvent;
                }
                LOGGER.error(new ObjectMessage(
                    Map.of(Constants.MESSAGE, "Unable to mock request since no default response found",
                        Constants.CUSTOMER_ID_FIELD, customerId, Constants.APP_FIELD, app
                        , Constants.INSTANCE_ID_FIELD, instanceId, Constants.SERVICE_FIELD, service,
                        Constants.METHOD_FIELD, method, Constants.PATH_FIELD, path, Constants.BODY,
                        body, Constants.REPLAY_ID_FIELD , replayId)));
                return Optional.empty();
            });

        respEvent.ifPresentOrElse(
            respEventVal -> createResponseFromEvent(mockRequestEvent, respEventVal),
            () -> {
                //TODO this is a hack : as ReqRespMatchResult is calculated from the perspective of
                //a recorded request, here in the mock we have a replay request which did not match
                //with any recorded request, but still to properly calculate no match counts for
                // virtualized services in facet queries, we are creating this dummy req resp
                // match result for now.
                // TODO change it back to MockReqNoMatch

                ReqRespMatchResult matchResult =
                    new ReqRespMatchResult(Optional.empty()
                        , Optional.ofNullable(mockRequestEvent.reqId), Comparator.MatchType.NoMatch
                        , 0, mockRequestEvent.getCollection(), mockRequestEvent.service
                        , mockRequestEvent.apiPath, Optional.empty()
                        , Optional.of(mockRequestEvent.getTraceId()), Optional.empty(),
                        Optional.empty(), Optional.ofNullable(mockRequestEvent.spanId),
                        Optional.ofNullable(mockRequestEvent.parentSpanId), new Comparator.Match(Comparator.MatchType
                        .Default, "", Collections.emptyList()), new Comparator.Match(Comparator.MatchType
                        .Default, "", Collections.emptyList()));
                cube.saveResult(matchResult);
            });

        return respEvent;
    }


    private static EventQuery buildFunctionEventQuery(Event event, int offset, int limit, boolean isSortOrderAsc) {
        return new EventQuery.Builder(event.customerId, event.app, Event.EventType.JavaRequest)
            .withService(event.service).withInstanceId(event.instanceId)
            .withPaths(List.of(event.apiPath)).withTraceId(event.getTraceId()).withTimestamp(event.timestamp)
            .withCollection(event.getCollection()).withPayloadKey(event.payloadKey)
            .withOffset(offset).withLimit(limit).withSortOrderAsc(isSortOrderAsc)
            .build();
    }

    private boolean setFunctionPayloadKeyAndCollection(Event event) {
        if (event != null && event.eventType.equals(Event.EventType.JavaRequest)) {
            Optional<ReqRespStore.RecordOrReplay> recordOrReplay = cube.getCurrentRecordOrReplay(
                event.customerId, event.app, event.instanceId);
            Optional<String> collection = recordOrReplay
                .flatMap(ReqRespStore.RecordOrReplay::getRecordingCollection);
            // check collection, validate, fetch template for request, set key and store. If error at any point stop
            if (collection.isPresent()) {
                event.setCollection(collection.get());
                try {
                    event.parseAndSetKey(cube
                        .getRequestMatchTemplate(event, recordOrReplay.get().getTemplateVersion()));
                } catch (ComparatorCache.TemplateNotFoundException e) {
                    LOGGER.error(new ObjectMessage(
                        Map.of("message", "Compare template not found.",
                            "type", event.eventType,
                            "reqId", event.reqId,
                            "path", event.apiPath)), e);
                    return false;
                }
                return true;
            } else {
                LOGGER
                    .error(new ObjectMessage(Map.of(
                        Constants.REASON, "Collection not found",
                        Constants.CUSTOMER_ID_FIELD, event.customerId,
                        Constants.APP_FIELD, event.app,
                        Constants.INSTANCE_ID_FIELD, event.instanceId,
                        Constants.TRACE_ID_FIELD, event.getTraceId())));
                return false;
            }
        } else {
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.REASON,
                "Invalid event - either event is null, or some required field missing, or both binary "
                    +
                    "and string payloads set")));
            return false;
        }
    }

    private FnResponse getFuncResp(Event event, long matchingEventsCount, Event retEvent) throws MockerException {
        LOGGER.debug(new ObjectMessage(
            Map.of(
                Constants.API_PATH_FIELD, retEvent.apiPath,
                Constants.TRACE_ID_FIELD, retEvent.getTraceId()/*,
                Constants.DATA, retEvent.payload.rawPayloadAsString()*/)));
        try {
            FnResponse fnResponse = new FnResponse(
                retEvent.payload.getValAsString(Constants.FN_RESPONSE_PATH),
                Optional.of(retEvent.timestamp),
                Success, Optional.empty(),
                matchingEventsCount > 1);
            return fnResponse;
        } catch (Exception e) {
            LOGGER.error(new ObjectMessage(
                Map.of(
                    Constants.API_PATH_FIELD, event.apiPath,
                    Constants.TRACE_ID_FIELD, event.getTraceId())) , e);
            throw new MockerException(Constants.JSON_PARSING_EXCEPTION,
                "Unable to find response path in json ");
        }
    }

    private FnResponse getDefaultFuncResp(Event event) throws MockerException {
        String errorReason = "Unable to find matching request, looking for default response";
        LOGGER.error(new ObjectMessage(
            Map.of(
                Constants.API_PATH_FIELD, event.apiPath,
                Constants.TRACE_ID_FIELD, event.getTraceId(),
                Constants.REASON, errorReason)));

        EventQuery.Builder defEventQuery = new EventQuery.Builder(event.customerId,
            event.app, event.eventType);
        defEventQuery.withService(event.service);
        defEventQuery.withRunType(Event.RunType.Manual);
        defEventQuery.withPaths(List.of(event.apiPath));

        Optional<Event> defaultRespEvent = cube.getSingleEvent(defEventQuery.build());
        // TODO revisit this logic once FnReqRespPayload is in place
        if (defaultRespEvent.isPresent()) {
            try {
                FnReqRespPayload fnReqRespPayload = (FnReqRespPayload) defaultRespEvent.get().payload;
                FnResponse fnResponse = new FnResponse(
                    fnReqRespPayload
                        .getValAsString(Constants.FN_RESPONSE_PATH),
                    Optional.of(defaultRespEvent.get().timestamp),
                    Success, Optional.empty(),
                    false);
                return fnResponse;
            } catch (DataObj.PathNotFoundException e) {
                LOGGER.error(new ObjectMessage(
                    Map.of(Constants.API_PATH_FIELD, event.apiPath)), e);
                throw new MockerException(Constants.JSON_PARSING_EXCEPTION, "Unable to find response path in json ");
            }
        }

        errorReason = "Unable to find default response!";
        LOGGER.error(new ObjectMessage(
            Map.of(
                Constants.API_PATH_FIELD, event.apiPath,
                Constants.TRACE_ID_FIELD, event.getTraceId(),
                Constants.REASON, errorReason)));
        throw new MockerException(Constants.EVENT_NOT_FOUND,
            errorReason);
    }

    static private Event createRequestMockNew(String path, MultivaluedMap<String, String> formParams,
                                              String customerId, String app, String instanceId, String service,
                                              String method, String body,
                                              MultivaluedMap<String, String> headers, MultivaluedMap<String, String> queryParams,
                                              String replayId, Comparator comparator) throws Event.EventBuilder.InvalidEventException, JsonProcessingException {
        // At the time of mock, our lua filters don't get deployed, hence no request id is generated
        // we can generate a new request id here in the mock service
        Optional<String> requestId = Optional.of(service.concat("-mock-").concat(String.valueOf(UUID.randomUUID())));

        MultivaluedMap<String, String> meta = new MultivaluedHashMap<>();
        meta.putSingle(Constants.SERVICE_FIELD, service);
        meta.putSingle(Constants.INSTANCE_ID_FIELD, instanceId);
        setSpanTraceIDParentSpanInMeta(meta, headers);
        return Utils.createHTTPRequestEvent(path, requestId, queryParams, formParams, meta, headers,
            method, body, Optional.of(replayId), Instant.now(), Optional.of(Event.RunType.Replay), Optional.of(customerId),
            Optional.of(app), comparator);

    }

    static private void setSpanTraceIDParentSpanInMeta(MultivaluedMap<String, String> meta, MultivaluedMap<String, String> headers) {
        String mdTrace = headers.getFirst(Constants.MD_TRACE_FIELD);
        if (mdTrace != null && !mdTrace.equals("")) {
            String[] parts = decodedValue(mdTrace).split(":");
            if (parts.length != 4) {
                LOGGER.warn("trace id should have 4 parts but found: " + parts.length);
                return;
            } else {
                String traceId = parts[0];
                if (traceId.length() <= 32 && traceId.length() >= 1) {
                    meta.putSingle(Constants.DEFAULT_SPAN_FIELD, Long.toHexString((new BigInteger(parts[1], 16)).longValue()));
                    meta.putSingle(Constants.DEFAULT_TRACE_FIELD, convertTraceId(high(parts[0]), (new BigInteger(parts[0], 16)).longValue()));
                } else {
                    LOGGER.error("Trace id [" + traceId + "] length is not within 1 and 32");
                }
            }
        } else if ( headers.getFirst(Constants.DEFAULT_TRACE_FIELD) != null ) {
            meta.putSingle(Constants.DEFAULT_TRACE_FIELD, headers.getFirst(Constants.DEFAULT_TRACE_FIELD));
            if ( headers.getFirst(Constants.DEFAULT_SPAN_FIELD) != null) {
                meta.putSingle(Constants.DEFAULT_SPAN_FIELD, decodedValue(headers.getFirst(Constants.DEFAULT_SPAN_FIELD)));
            }
        } else {
            LOGGER.warn("Neither default not md trace id header found to the mock sever request");
        }

        if (headers.getFirst(Constants.MD_BAGGAGE_PARENT_SPAN) != null ) {
            meta.putSingle(Constants.DEFAULT_PARENT_SPAN_FIELD, decodedValue(headers.getFirst(Constants.MD_BAGGAGE_PARENT_SPAN)));
        } else if (headers.getFirst(Constants.DEFAULT_BAGGAGE_PARENT_SPAN) != null ) {
            meta.putSingle(Constants.DEFAULT_PARENT_SPAN_FIELD, decodedValue(headers.getFirst(Constants.DEFAULT_BAGGAGE_PARENT_SPAN)));
        } else {
            LOGGER.warn("Neither default not md baggage parent span id header found to the mock sever request");
        }
    }

    static private String decodedValue(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException var3) {
            return value;
        }
    }

    static private String convertTraceId(long traceIdHigh, long traceIdLow) {
        if (traceIdHigh == 0L) {
            return Long.toHexString(traceIdLow);
        }
        final String hexStringHigh = Long.toHexString(traceIdHigh);
        final String hexStringLow = Long.toHexString(traceIdLow);
        if (hexStringLow.length() < 16) {
            // left pad low trace id with '0'.
            // In theory, only 12.5% of all possible long values will be padded.
            // In practice, using Random.nextLong(), only 6% will need padding
            return hexStringHigh + "0000000000000000".substring(hexStringLow.length()) + hexStringLow;
        }
        return hexStringHigh + hexStringLow;
    }

    static private long high(String hexString) {
        if (hexString.length() > 16) {
            int highLength = hexString.length() - 16;
            String highString = hexString.substring(0, highLength);
            return (new BigInteger(highString, 16)).longValue();
        } else {
            return 0L;
        }
    }

    static private EventQuery getRequestEventQuery(Event mockRequest, String collection, int limit) {
        String customerId = mockRequest.customerId;
        String app = mockRequest.app;
        EventQuery.Builder builder = new EventQuery.Builder(customerId, app, Event.EventType.HTTPRequest);
        return builder
            .withCollection(collection)
            .withService(mockRequest.service)
            .withTraceId(mockRequest.getTraceId())
            .withPaths(List.of(mockRequest.apiPath))
            .withPayloadKey(mockRequest.payloadKey)
            .withRunType(Event.RunType.Record)
            .withSortOrderAsc(true)
            .withLimit(limit)
            .build();
    }

    static private EventQuery getDefaultRespEventQuery(Event mockRequest) {
        EventQuery.Builder eventQuery = new EventQuery.Builder(
            mockRequest.customerId,
            mockRequest.app, Event.EventType.HTTPResponse);
        return eventQuery
            .withService(mockRequest.service)
            .withPaths(List.of(mockRequest.apiPath))
            .withRunType(Event.RunType.Manual)
            .build();
    }

    private void createResponseFromEvent(
        Event mockRequestEvent, Event respEventVal) {

        // store a req-resp analysis match result for the mock request (during replay)
        // and the matched recording request
        String recordReqId = respEventVal.reqId;
        ReqRespMatchResult matchResult =
            new ReqRespMatchResult(Optional.ofNullable(recordReqId)
                , Optional.ofNullable(mockRequestEvent.reqId), Comparator.MatchType.ExactMatch
                , 1, mockRequestEvent.getCollection(), mockRequestEvent.service
                , mockRequestEvent.apiPath, Optional.of(respEventVal.getTraceId())
                , Optional.of(mockRequestEvent.getTraceId()), Optional.ofNullable(respEventVal.spanId),
                Optional.ofNullable(respEventVal.parentSpanId), Optional.ofNullable(mockRequestEvent.spanId),
                Optional.ofNullable(mockRequestEvent.parentSpanId), new Comparator.Match(Comparator.MatchType
                .ExactMatch, "", Collections.emptyList()), new Comparator.Match(Comparator.MatchType
                .ExactMatch, "", Collections.emptyList()));
        cube.saveResult(matchResult);
        try {
            Event mockResponseToStore = createMockResponseEvent(respEventVal,
                Optional.of(mockRequestEvent.reqId),
                mockRequestEvent.instanceId, mockRequestEvent.getCollection());
            cube.save(mockResponseToStore);
        } catch (Event.EventBuilder.InvalidEventException e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Not able to store mock response event",
                    Constants.TRACE_ID_FIELD, respEventVal.getTraceId(),
                    Constants.REQ_ID_FIELD, respEventVal.reqId)));
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
            originalResponse.apiPath, Event.EventType.HTTPResponse);
        return builder.setPayload(originalResponse.payload).createEvent();
    }

}
