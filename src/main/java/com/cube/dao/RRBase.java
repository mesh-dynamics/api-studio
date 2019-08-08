/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.cube.core.*;
import io.cube.agent.CommonUtils;

public class RRBase {

	public static final String REQIDPATH = "/reqid";
	public static final String COLLECTIONPATH = "/collection";
	public static final String TIMESTAMPPATH = "/timestamp";
	public static final String RRTYPEPATH = "/rrtype";
	public static final String CUSTOMERIDPATH = "/customerid";
	public static final String APPPATH = "/app";

	public static enum RR {
		Record,
		Replay,
		Manual  // manually created e.g. default requests and responses
	}

	/**
	 * @param reqid
	 * @param meta
	 * @param hdrs
	 * @param body
	 * @param collection
	 * @param timestamp
	 * @param rrtype
	 * @param customerid
	 * @param app
	 */
	public RRBase(Optional<String> reqid,
			MultivaluedMap<String, String> meta,
			MultivaluedMap<String, String> hdrs,
			String body,
			Optional<String> collection,
			Optional<Instant> timestamp,
			Optional<RR> rrtype,
			Optional<String> customerid,
			Optional<String> app) {
		super();
		this.reqid = reqid;
		this.meta = meta != null ? meta : new MultivaluedHashMap<String, String>();
		this.hdrs = hdrs != null ? hdrs : new MultivaluedHashMap<String, String>();
		this.body = body;
		this.collection = collection;
		this.timestamp = timestamp;
		this.rrtype = rrtype;
		this.customerid = customerid;
		this.app = app;
	}


	/**
	 * For jackson json ser/deserialization
	 */
	@SuppressWarnings("unused") RRBase() {
		super();
		this.reqid = Optional.empty();
		this.meta = new MultivaluedHashMap<String, String>();
		this.hdrs = new MultivaluedHashMap<String, String>();
		this.body = "";
		this.collection = Optional.empty();
		this.timestamp = Optional.empty();
		this.rrtype = Optional.empty();
		this.customerid = Optional.empty();
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

	protected Comparator.Match compare(RRBase rhs,
									   CompareTemplate template,
									   CompareTemplate metaFieldTemplate,
									   CompareTemplate hdrFieldTemplate,
									   Comparator bodyComparator,
									   boolean needDiff) {
		Comparator.Match match = new Comparator.Match(Comparator.MatchType.ExactMatch, "", new ArrayList<Comparator.Diff>());
		template.getRule("/reqid").checkMatchStr(reqid, rhs.reqid, match, needDiff);
		metaFieldTemplate.checkMatch(meta, rhs.meta, match, needDiff);
		hdrFieldTemplate.checkMatch(hdrs, rhs.hdrs, match, needDiff);
		if (getMimeType().equalsIgnoreCase(APPLICATION_JSON) || ((bodyComparator instanceof JsonComparator)
				&& ((JsonComparator) bodyComparator).pathRulesExist())) {
			match.merge(bodyComparator.compare(body, rhs.body), needDiff, BODYPATH);
		} else {
			// treat as simple string
			template.getRule("/body").checkMatchStr(body, rhs.body, match, needDiff);
		}
		template.getRule("/collection").checkMatchStr(collection, rhs.collection, match, needDiff);
		template.getRule("/timestamp").checkMatchStr(timestamp.toString(), rhs.timestamp.toString(), match, needDiff);
		template.getRule("/rrtype").checkMatchStr(rrtype.toString(), rhs.rrtype.toString(), match, needDiff);
		template.getRule("/customerid").checkMatchStr(customerid, rhs.customerid, match, needDiff);
		template.getRule("/app").checkMatchStr(app, rhs.app, match, needDiff);
		return match;
	}

    public String getMimeType() {
		return CommonUtils.getCaseInsensitiveMatches(hdrs , HttpHeaders.CONTENT_TYPE).stream()
            .findFirst().orElse(MediaType.TEXT_PLAIN);
    }


    public Optional<String> reqid;
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> meta;
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> hdrs;
	public final String body;
	public Optional<String> collection;
	public final Optional<Instant> timestamp;
	public Optional<RR> rrtype; // this can be "record" or "replay"
	public final Optional<String> customerid;
	public final Optional<String> app;

	public static final String SERVICEFIELD = "service";
	public static final String INSTANCEIDFIELD = "instanceid";
	public static final String HDRPATHFIELD = "_path";

	public static final String HDRPATH = "/hdr";
	public static final String METAPATH = "/meta";
	public static final String BODYPATH = "/body";

}
