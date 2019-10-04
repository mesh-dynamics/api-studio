/**
 * Copyright Cube I O
 */
package com.cube.dao;

import com.cube.core.Comparator;
import com.cube.core.Comparator.Match;
import com.cube.core.CompareTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Response extends RRBase {

    private static final Logger LOGGER = LogManager.getLogger(Response.class);

    /**
	 * @param reqid
	 * @param status
	 * @param hdrs
	 * @param body
	 */
	public Response(Optional<String> reqid, int status, 
			MultivaluedMap<String, String> meta, 
			MultivaluedMap<String, String> hdrs, String body,
			Optional<String> collection,
			Optional<Instant> timestamp, 
			Optional<RR> rrtype, 
			Optional<String> customerid,
			Optional<String> app) {
		super(reqid, meta, hdrs, body, collection, timestamp, rrtype, customerid, app);
		this.status = status;
	}
	
	public Response(Optional<String> reqid, int status,
			String body,
			Optional<String> collection,
			Optional<String> customerid,
			Optional<String> app,
			Optional<String> contenttype) {
		this(reqid, status, emptyMap(), emptyMap(), body, collection, Optional.empty(), Optional.empty(),
				customerid, app);
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


	public static Optional<Response> fromEvent(Event event, ObjectMapper jsonmapper) {
	    if (event.eventType != Event.EventType.HTTPResponse) {
	        LOGGER.error(String.format("Not able to convert event to response. Event %s not of right type: ",
                event.reqId, event.eventType.toString()));
	        return Optional.empty();
        }
        try {
            HTTPResponsePayload responsePayload = jsonmapper.readValue(event.rawPayloadString, HTTPResponsePayload.class);
            return Optional.of(new Response(Optional.of(event.reqId), responsePayload.status, emptyMap(),
                responsePayload.hdrs,
                responsePayload.body, Optional.of(event.getCollection()), Optional.of(event.timestamp),
                Optional.of(event.rrType), Optional.of(event.customerId), Optional.of(event.app)));
        } catch (IOException e) {
            LOGGER.error(String.format("Not able to convert event with reqid: %s and type %s to response. ",
                event.reqId, event.eventType.toString()));
            return Optional.empty();
        }
    }
}
