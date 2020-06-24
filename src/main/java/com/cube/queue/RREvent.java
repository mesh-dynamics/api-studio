package com.cube.queue;

import javax.ws.rs.core.MultivaluedMap;

import io.md.dao.MDStorable;

import com.cube.dao.ReqRespStore.ReqResp;

public class RREvent implements MDStorable {

	public final ReqResp rr;
	public final String path;
	public final MultivaluedMap<String, String> queryParams;

	public RREvent(ReqResp rr, String path, MultivaluedMap<String, String> queryParams) {
		this.rr = rr;
		this.path = path;
		this.queryParams = queryParams;
	}

}
