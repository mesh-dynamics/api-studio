/**
 * Copyright Cube I O
 */
package com.cube.dao;

import static com.cube.dao.Event.RecordReplayType.Record;

import java.time.Instant;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.cube.core.Comparator;
import com.cube.core.Comparator.MatchType;
import com.cube.core.CompareTemplate;
import com.cube.core.RequestComparator;
import com.cube.ws.Config;

public class Request extends RRBase {
	public static final String QPARAMPATH = "/qparams";
	public static final String FPARAMPATH = "/fparams";
	public static final String PATHPATH = "/path";
	public static final String METHODPATH = "/method";
	public static final String ARGSPATH = "/args";
	public static final String FNRESPONSEPATH = "/response";


	/**
	 * @param path
	 * @param reqid
	 * @param qparams
	 * @param meta
	 * @param hdrs
	 * @param body
	 */
	public Request(String path, Optional<String> reqid, 
			MultivaluedMap<String, String> qparams,
			MultivaluedMap<String, String> fparams,
			MultivaluedMap<String, String> meta, 
			MultivaluedMap<String, String> hdrs, 
			String method, 
			String body,
			Optional<String> collection,
			Optional<Instant> timestamp, 
			Optional<Event.RecordReplayType> rrtype,
			Optional<String> customerid,
			Optional<String> app) {
		super(reqid, meta, hdrs, body, collection, timestamp, rrtype, customerid, app);
		this.path = path; 
		this.qparams = qparams != null ? qparams : emptyMap();
		this.fparams = fparams != null ? fparams : emptyMap();
		this.method = method;
	}
	
	
	
	/**
	 * @param path
	 * @param qparams
	 * @param fparams
	 */
	public Request(String path, Optional<String> id, 
			MultivaluedMap<String, String> qparams, 
			MultivaluedMap<String, String> fparams, 
			MultivaluedMap<String, String> hdrs, 
			String service, 
			Optional<String> collection, 
			Optional<Event.RecordReplayType> rrtype,
			Optional<String> customerid,
			Optional<String> app) {
		this(path, id, qparams, fparams, emptyMap(), 
				hdrs, "", "", collection, Optional.empty(), rrtype, customerid, app);
		meta.add(RRBase.SERVICEFIELD, service);
	}

	public Request(Optional<String> serviceid, 
			String path,
			String method,
			Optional<Event.RecordReplayType> rrtype,
			Optional<String> customerid,
			Optional<String> app) {
		this(path, Optional.empty(), emptyMap(), emptyMap(), emptyMap(), 
				emptyMap(), method, "", Optional.empty(), Optional.empty(), rrtype, customerid, app);
		serviceid.ifPresent(s -> setService(s));
	}
	
	
	
	/**
	 * For jackson json ser/deserialization
	 */
	@SuppressWarnings("unused")
	private Request() {
		super();
		this.path = ""; 
		this.qparams = new MultivaluedHashMap<String, String>();
		this.fparams = new MultivaluedHashMap<String, String>();
		this.method = "";
	}



	static final TypeReference<MultivaluedHashMap<String, String>> typeRef 
	  = new TypeReference<MultivaluedHashMap<String, String>>() {};
	
	public final String path;
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> qparams; // query params
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> fparams; // form params
	public final String method;

	private static MultivaluedHashMap<String, String> emptyMap () {
		return new MultivaluedHashMap<String, String>();
	}

    public Event toEvent(RequestComparator comparator, Config config)
        throws JsonProcessingException, EventBuilder.InvalidEventException {

        HTTPRequestPayload payload = new HTTPRequestPayload(hdrs, qparams, fparams,
            method, body);
        String payloadStr;
        payloadStr = config.jsonmapper.writeValueAsString(payload);

        EventBuilder eventBuilder = new EventBuilder(customerid.orElse("NA"), app.orElse("NA"),
            getService().orElse("NA"), getInstance().orElse("NA"), collection.orElse("NA"),
            getTraceId().orElse("NA"), rrtype.orElse(Record), timestamp.orElse(Instant.now()),
            reqid.orElse(
                "NA"),
            path, Event.EventType.HTTPRequest);
        eventBuilder.setRawPayloadString(payloadStr);
        Event event = eventBuilder.createEvent();
        event.parseAndSetKey(config, comparator.getCompareTemplate());

        return event;
    }

    public MatchType compare(Request rhs, CompareTemplate template, CompareTemplate metaFieldtemplate, CompareTemplate hdrFieldTemplate,
							 Comparator bodyComparator, CompareTemplate qparamFieldTemplate, CompareTemplate fparamFieldTemplate) {

		// diff not needed, so pass false
		Comparator.Match match = super.compare(rhs, template, metaFieldtemplate, hdrFieldTemplate, bodyComparator, false);
		template.getRule("/path").checkMatchStr(path, rhs.path, match, false);
		qparamFieldTemplate.checkMatch(qparams, rhs.qparams, match, false);
		fparamFieldTemplate.checkMatch(fparams, rhs.fparams, match, false);
		template.getRule("/method").checkMatchStr(method, rhs.method, match, false);

		return match.mt;
	}

}
