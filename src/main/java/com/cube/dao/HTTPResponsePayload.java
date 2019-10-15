/*
 *
 *    Copyright Cube I O
 *
 */
package com.cube.dao;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-10-01
 */
public class HTTPResponsePayload {

    /**
     *
     * @param hdrs
     * @param status
     * @param body
     */
	public HTTPResponsePayload(MultivaluedMap<String, String> hdrs,
                               int status,
                               String body) {
	    this.hdrs = hdrs;
	    this.status = status;
		this.body = body;
    }



	/**
	 * For jackson json ser/deserialization
	 */
	@SuppressWarnings("unused")
	private HTTPResponsePayload() {
		super();
		this.hdrs = new MultivaluedHashMap<String, String>();
		this.status = Response.Status.OK.getStatusCode();
		this.body = "";
	}


    @JsonDeserialize(as=MultivaluedHashMap.class)
    public final MultivaluedMap<String, String> hdrs;
	public final int status;
    public final String body;

	private static MultivaluedHashMap<String, String> emptyMap () {
		return new MultivaluedHashMap<String, String>();
	}

}
