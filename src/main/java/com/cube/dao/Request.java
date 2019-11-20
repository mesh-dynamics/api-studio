/**
 * Copyright Cube I O
 */
package com.cube.dao;

import static com.cube.dao.Event.RunType.Record;

import com.cube.utils.Constants;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.cube.core.Comparator;
import com.cube.core.Comparator.MatchType;
import com.cube.core.CompareTemplate;
import com.cube.ws.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

// TODO: Event redesign: This can be removed
public class Request extends RRBase {
    private static final Logger LOGGER = LogManager.getLogger(Request.class);


	/**
	 * @param apiPath
	 * @param reqId
	 * @param queryParams
	 * @param meta
	 * @param hdrs
	 * @param body
	 */
	public Request(String apiPath, Optional<String> reqId,
			MultivaluedMap<String, String> queryParams,
			MultivaluedMap<String, String> formParams,
			MultivaluedMap<String, String> meta,
			MultivaluedMap<String, String> hdrs,
			String method,
			String body,
			Optional<String> collection,
			Optional<Instant> timestamp,
			Optional<Event.RunType> runType,
			Optional<String> customerId,
			Optional<String> app) {
		super(reqId, meta, hdrs, body, collection, timestamp, runType, customerId, app, apiPath);
        this.queryParams = queryParams != null ? queryParams : emptyMap();
		this.formParams = formParams != null ? formParams : emptyMap();
		this.method = method;
	}



	/**
	 * @param apiPath
	 * @param queryParams
	 * @param formParams
	 */
	public Request(String apiPath, Optional<String> id,
			MultivaluedMap<String, String> queryParams,
			MultivaluedMap<String, String> formParams,
			MultivaluedMap<String, String> hdrs,
			String service,
			String method,
			String body,
			Optional<String> collection,
			Optional<Event.RunType> runType,
			Optional<String> customerId,
			Optional<String> app) {
		this(apiPath, id, queryParams, formParams, emptyMap(),
				hdrs, method, body, collection, Optional.empty(), runType, customerId, app);
		meta.add(Constants.SERVICE_FIELD, service);
	}

	public Request(Optional<String> serviceid,
			String path,
			String method,
			Optional<Event.RunType> runType,
			Optional<String> customerId,
			Optional<String> app) {
		this(path, Optional.empty(), emptyMap(), emptyMap(), emptyMap(),
				emptyMap(), method, "", Optional.empty(), Optional.empty(), runType, customerId, app);
		serviceid.ifPresent(s -> setService(s));
	}



	/**
	 * For jackson json ser/deserialization
	 */
	@SuppressWarnings("unused")
	private Request() {
		super();
        this.queryParams = new MultivaluedHashMap<String, String>();
		this.formParams = new MultivaluedHashMap<String, String>();
		this.method = "";
	}

	static final TypeReference<MultivaluedHashMap<String, String>> typeRef
	  = new TypeReference<MultivaluedHashMap<String, String>>() {};

    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> queryParams; // query params
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> formParams; // form params
	public final String method;

	private static MultivaluedHashMap<String, String> emptyMap () {
		return new MultivaluedHashMap<String, String>();
	}

    public Event toEvent(Comparator comparator, Config config)
        throws JsonProcessingException, Event.EventBuilder.InvalidEventException {

        HTTPRequestPayload payload = new HTTPRequestPayload(hdrs, queryParams, formParams,
            method, body);
        String payloadStr;
        payloadStr = config.jsonMapper.writeValueAsString(payload);

        Event.EventBuilder eventBuilder = new Event.EventBuilder(customerId.orElse("NA"), app.orElse("NA"),
            getService().orElse("NA"), getInstance().orElse("NA"), collection.orElse("NA"),
            getTraceId().orElse("NA"), runType.orElse(Record), timestamp.orElse(Instant.now()),
            reqId.orElse(
                "NA"),
            apiPath, Event.EventType.HTTPRequest);
        eventBuilder.setRawPayloadString(payloadStr);
        Event event = eventBuilder.createEvent();
        event.parseAndSetKey(config, comparator.getCompareTemplate());

        return event;
    }

    public static Optional<Request> fromEvent(Event event, ObjectMapper jsonMapper) {
        if (event.eventType != Event.EventType.HTTPRequest) {
            LOGGER.error(new ObjectMessage(Map.of("reason" , "Not able to convert event to request. " +
                    "Event is not of right type:" , "eventType"
                , event.eventType.toString() , "reqId", event.reqId)));
            return Optional.empty();
        }

        try {
            HTTPRequestPayload payload = jsonMapper.readValue(event.rawPayloadString, HTTPRequestPayload.class);
            MultivaluedHashMap<String, String> meta = new MultivaluedHashMap<>();
            meta.put(Constants.SERVICE_FIELD, List.of(event.service));
            meta.put(Constants.INSTANCE_ID_FIELD, List.of(event.instanceId));
            return Optional.of(new Request(event.apiPath, Optional.of(event.reqId), payload.queryParams, payload.formParams,
                meta, payload.hdrs, payload.method, payload.body,
                Optional.of(event.getCollection()), Optional.of(event.timestamp),
                Optional.of(event.runType), Optional.of(event.customerId), Optional.of(event.app)));
        } catch (IOException e) {
            LOGGER.error(new ObjectMessage(Map.of("reason" , "Not able to convert Event to Request",
                "eventType", event.eventType.toString() , "reqId", event.reqId)));
            return Optional.empty();
        }
    }

    public MatchType compare(Request rhs, CompareTemplate template, CompareTemplate metaFieldtemplate, CompareTemplate hdrFieldTemplate,
							 Comparator bodyComparator, CompareTemplate qparamFieldTemplate, CompareTemplate fparamFieldTemplate) {

		// diff not needed, so pass false
		Comparator.Match match = super.compare(rhs, template, metaFieldtemplate, hdrFieldTemplate, bodyComparator, false);
		template.getRule(Constants.API_PATH_PATH).checkMatchStr(apiPath, rhs.apiPath, match, false);
		qparamFieldTemplate.checkMatch(queryParams, rhs.queryParams, match, false);
		fparamFieldTemplate.checkMatch(formParams, rhs.formParams, match, false);
		template.getRule(Constants.METHOD_PATH).checkMatchStr(method, rhs.method, match, false);

		return match.mt;
	}

}
