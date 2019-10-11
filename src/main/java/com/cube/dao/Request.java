/**
 * Copyright Cube I O
 */
package com.cube.dao;

import static com.cube.dao.Event.RunType.Record;

import java.io.IOException;
import java.time.Instant;
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
import com.cube.core.RequestComparator;
import com.cube.ws.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

public class Request extends RRBase {
    private static final Logger LOGGER = LogManager.getLogger(Request.class);

	public static final String QPARAMPATH = "/queryParams";
	public static final String FPARAMPATH = "/formParams";
	public static final String PATHPATH = "/path";
	public static final String METHODPATH = "/method";
	public static final String ARGSPATH = "/args";
	public static final String FNRESPONSEPATH = "/response";


	/**
	 * @param path
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
		super(reqId, meta, hdrs, body, collection, timestamp, runType, customerId, app);
		this.apiPath = apiPath;
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
			Optional<String> collection, 
			Optional<Event.RunType> runType,
			Optional<String> customerId,
			Optional<String> app) {
		this(apiPath, id, queryParams, formParams, emptyMap(),
				hdrs, "", "", collection, Optional.empty(), runType, customerId, app);
		meta.add(RRBase.SERVICEFIELD, service);
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
		this.apiPath = "";
		this.queryParams = new MultivaluedHashMap<String, String>();
		this.formParams = new MultivaluedHashMap<String, String>();
		this.method = "";
	}

	static final TypeReference<MultivaluedHashMap<String, String>> typeRef 
	  = new TypeReference<MultivaluedHashMap<String, String>>() {};
	
	public final String apiPath;
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> queryParams; // query params
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> formParams; // form params
	public final String method;

	private static MultivaluedHashMap<String, String> emptyMap () {
		return new MultivaluedHashMap<String, String>();
	}

    public Event toEvent(RequestComparator comparator, Config config)
        throws JsonProcessingException, EventBuilder.InvalidEventException {

        HTTPRequestPayload payload = new HTTPRequestPayload(hdrs, queryParams, formParams,
            method, body);
        String payloadStr;
        payloadStr = config.jsonMapper.writeValueAsString(payload);

        EventBuilder eventBuilder = new EventBuilder(customerId.orElse("NA"), app.orElse("NA"),
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
            return Optional.of(new Request(event.apiPath, Optional.of(event.reqId), payload.queryParams, payload.formParams,
                new MultivaluedHashMap<>(), payload.hdrs, payload.method, payload.body,
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
		template.getRule("/apiPath").checkMatchStr(apiPath, rhs.apiPath, match, false);
		qparamFieldTemplate.checkMatch(queryParams, rhs.queryParams, match, false);
		fparamFieldTemplate.checkMatch(formParams, rhs.formParams, match, false);
		template.getRule("/method").checkMatchStr(method, rhs.method, match, false);

		return match.mt;
	}

}
