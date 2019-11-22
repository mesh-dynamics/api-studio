/**
 * Copyright Cube I O
 */
package com.cube.dao;

import static com.cube.dao.Event.RunType.Record;

import com.cube.core.Comparator;
import com.cube.core.Comparator.Match;
import com.cube.core.CompareTemplate;
import com.cube.dao.DataObj.DataObjCreationException;
import com.cube.exception.DataObjException;
import com.cube.utils.Constants;
import com.cube.ws.Config;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// TODO: Event redesign: This can be removed
public class Response extends RRBase {

    private static final Logger LOGGER = LogManager.getLogger(Response.class);

    /**
	 * @param reqId
	 * @param status
	 * @param hdrs
	 * @param body
	 */
	public Response(Optional<String> reqId, int status,
			MultivaluedMap<String, String> meta,
			MultivaluedMap<String, String> hdrs, String body,
			Optional<String> collection,
			Optional<Instant> timestamp,
			Optional<Event.RunType> runType,
			Optional<String> customerId,
			Optional<String> app, String apiPath) {
		super(reqId, meta, hdrs, body, collection, timestamp, runType, customerId, app, apiPath);
		this.status = status;
	}

	public Response(Optional<String> reqId, int status,
			String body,
			Optional<String> collection,
			Optional<String> customerId,
			Optional<String> app,
			Optional<String> contenttype, String apiPath) {
		this(reqId, status, emptyMap(), emptyMap(), body, collection, Optional.empty(), Optional.empty(),
				customerId, app, apiPath);
		contenttype.ifPresent(ct -> hdrs.add(HttpHeaders.CONTENT_TYPE, ct));
	}

	/**
	 *
	 */
	@SuppressWarnings("unused")
	private Response() {
		super();
		this.status = Status.OK.getStatusCode();
	}


	public final int status;

	private static final MultivaluedHashMap<String, String> emptyMap() {
		return new MultivaluedHashMap<String, String>();
	}

	public Match compare(Response rhs, CompareTemplate template, CompareTemplate metaFieldTemplate,
									CompareTemplate hdrFieldTemplate, Comparator bodyComparator) {
		Match match = super.compare(rhs, template, metaFieldTemplate, hdrFieldTemplate, bodyComparator, true);
		template.getRule("/status").checkMatchInt(status, rhs.status, match, true);
		return match;
	}


	public static Optional<Response> fromEvent(Event event, ObjectMapper jsonMapper) {
	    if (event.eventType != Event.EventType.HTTPResponse) {
	        LOGGER.error(String.format("Not able to convert event to response. Event %s not of right type: ",
                event.reqId, event.eventType.toString()));
	        return Optional.empty();
        }
        try {
            HTTPResponsePayload responsePayload = jsonMapper.readValue(event.rawPayloadString, HTTPResponsePayload.class);
            MultivaluedHashMap<String, String> meta = new MultivaluedHashMap<>();
            meta.put(Constants.SERVICE_FIELD, List.of(event.service));
            meta.put(Constants.INSTANCE_ID_FIELD, List.of(event.instanceId));
            meta.put(Config.DEFAULT_TRACE_FIELD, List.of(event.traceId));

            return Optional.of(new Response(Optional.of(event.reqId), responsePayload.status, meta,
                responsePayload.hdrs,
                responsePayload.body, Optional.of(event.getCollection()), Optional.of(event.timestamp),
                Optional.of(event.runType), Optional.of(event.customerId), Optional.of(event.app), event.apiPath));
        } catch (IOException e) {
            LOGGER.error(String.format("Not able to convert event with reqId: %s and type %s to response. ",
                event.reqId, event.eventType.toString()));
            return Optional.empty();
        }
    }

    public Event toEvent(Config config, String apiPath)
        throws JsonProcessingException, Event.EventBuilder.InvalidEventException {

        HTTPResponsePayload payload = new HTTPResponsePayload(hdrs, status, body);
        String payloadStr;
        payloadStr = config.jsonMapper.writeValueAsString(payload);

        Event.EventBuilder eventBuilder = new Event.EventBuilder(customerId.orElse("NA"), app.orElse("NA"),
            getService().orElse("NA"), getInstance().orElse("NA"), collection.orElse("NA"),
            getMetaField(Config.DEFAULT_TRACE_FIELD).orElse("NA"), runType.orElse(Record), timestamp.orElse(Instant.now()),
            reqId.orElse("NA"), apiPath, Event.EventType.HTTPResponse);
        eventBuilder.setRawPayloadString(payloadStr);
        Event event = eventBuilder.createEvent();
		try {
			event.parsePayLoad(config);
		} catch (DataObjCreationException e) {
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.EVENT_TYPE_FIELD, event.eventType,
					Constants.REQ_ID_FIELD, event.reqId)), e);
		}

		return event;
    }



}
