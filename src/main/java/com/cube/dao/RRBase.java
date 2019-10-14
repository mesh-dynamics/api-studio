/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.cube.core.*;
import com.cube.ws.Config;

import io.cube.agent.CommonUtils;

// TODO: Event redesign: This can be removed
public class RRBase {

	public static final String REQIDPATH = "/reqId";
	public static final String COLLECTIONPATH = "/collection";
	public static final String TIMESTAMPPATH = "/timestamp";
	public static final String RUNTYPEPATH = "/runType";
	public static final String CUSTOMERIDPATH = "/customerId";
	public static final String APPPATH = "/app";


    /**
	 * @param reqId
	 * @param meta
	 * @param hdrs
	 * @param body
	 * @param collection
	 * @param timestamp
	 * @param runType
	 * @param customerId
	 * @param app
	 */
	public RRBase(Optional<String> reqId,
			MultivaluedMap<String, String> meta,
			MultivaluedMap<String, String> hdrs,
			String body,
			Optional<String> collection,
			Optional<Instant> timestamp,
			Optional<Event.RunType> runType,
			Optional<String> customerId,
			Optional<String> app) {
		super();
		this.reqId = reqId;
		this.meta = meta != null ? meta : new MultivaluedHashMap<String, String>();
		this.hdrs = hdrs != null ? hdrs : new MultivaluedHashMap<String, String>();
		this.body = body;
		this.collection = collection;
		this.timestamp = timestamp;
		this.runType = runType;
		this.customerId = customerId;
		this.app = app;
	}


	/**
	 * For jackson json ser/deserialization
	 */
	@SuppressWarnings("unused") RRBase() {
		super();
		this.reqId = Optional.empty();
		this.meta = new MultivaluedHashMap<String, String>();
		this.hdrs = new MultivaluedHashMap<String, String>();
		this.body = "";
		this.collection = Optional.empty();
		this.timestamp = Optional.empty();
		this.runType = Optional.empty();
		this.customerId = Optional.empty();
		this.app = Optional.empty();
	}

	/**
	 * @return
	 */
	@JsonIgnore
	public Optional<String> getService() {
		return getMetaField(SERVICEFIELD);
	}

	public void setService(String serviceid) {
		setMetaField(SERVICEFIELD, serviceid);
	}

	@JsonIgnore
    public Optional<String> getTraceId() {
	    return getHdrField(Config.DEFAULT_TRACE_FIELD);
    }


    /**
	 * @return
	 */
	@JsonIgnore
	public Optional<String> getInstance() {
		return getMetaField(INSTANCEIDFIELD);
	}


	/**
	 * @return
	 */
	@JsonIgnore
	public Optional<String> getMetaField(String fieldname) {
		return Optional.ofNullable(meta.getFirst(fieldname));
	}

	public void setMetaField(String fieldname, String value) {
		meta.putSingle(fieldname, value);
	}


    /**
     * @return
     */
    @JsonIgnore
    private Optional<String> getHdrField(String fieldname) {
        return Optional.ofNullable(hdrs.getFirst(fieldname));
    }

	protected Comparator.Match compare(RRBase rhs,
									   CompareTemplate template,
									   CompareTemplate metaFieldTemplate,
									   CompareTemplate hdrFieldTemplate,
									   Comparator bodyComparator,
									   boolean needDiff) {
		Comparator.Match match = new Comparator.Match(Comparator.MatchType.ExactMatch, "", new ArrayList<Comparator.Diff>());
		template.getRule("/reqId").checkMatchStr(reqId, rhs.reqId, match, needDiff);
		metaFieldTemplate.checkMatch(meta, rhs.meta, match, needDiff);
		hdrFieldTemplate.checkMatch(hdrs, rhs.hdrs, match, needDiff);
		if ((getMimeType().equalsIgnoreCase(APPLICATION_JSON) || (bodyComparator instanceof JsonComparator))
				&& ((JsonComparator) bodyComparator).shouldConsiderAsObj()) {
			match.merge(bodyComparator.compare(body, rhs.body), needDiff, BODYPATH);
		} else {
			// treat as simple string
			template.getRule("/body").checkMatchStr(body, rhs.body, match, needDiff);
		}
		template.getRule("/collection").checkMatchStr(collection, rhs.collection, match, needDiff);
		template.getRule("/timestamp").checkMatchStr(timestamp.toString(), rhs.timestamp.toString(), match, needDiff);
		template.getRule("/runType").checkMatchStr(runType.toString(), rhs.runType.toString(), match, needDiff);
		template.getRule("/customerId").checkMatchStr(customerId, rhs.customerId, match, needDiff);
		template.getRule("/app").checkMatchStr(app, rhs.app, match, needDiff);
		return match;
	}

    public String getMimeType() {
		return CommonUtils.getCaseInsensitiveMatches(hdrs , HttpHeaders.CONTENT_TYPE).stream()
            .findFirst().orElse(MediaType.TEXT_PLAIN);
    }


    public Optional<String> reqId;
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> meta;
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> hdrs;
	public final String body;
	public Optional<String> collection;
	public final Optional<Instant> timestamp;
	public Optional<Event.RunType> runType; // this can be "record" or "replay"
	public final Optional<String> customerId;
	public final Optional<String> app;

	public static final String SERVICEFIELD = "service";
	public static final String INSTANCEIDFIELD = "instanceid";
	public static final String HDRPATHFIELD = "_path";

	public static final String HDRPATH = "/hdr";
	public static final String METAPATH = "/meta";
	public static final String BODYPATH = "/body";

}
