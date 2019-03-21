/**
 * Copyright Cube I O
 */
package com.cube.dao;

import com.cube.core.Comparator;
import com.cube.core.Comparator.Match;
import com.cube.core.CompareTemplate;

import java.time.Instant;
import java.util.Optional;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

public class Response extends RRBase {
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
}