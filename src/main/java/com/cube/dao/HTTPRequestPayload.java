/*
 *
 *    Copyright Cube I O
 *
 */
package com.cube.dao;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-10-01
 */
public class HTTPRequestPayload {

    /**
     *
     * @param hdrs
     * @param qparams
     * @param fparams
     * @param method
     * @param body
     */
	public HTTPRequestPayload(MultivaluedMap<String, String> hdrs,
                              MultivaluedMap<String, String> qparams,
                              MultivaluedMap<String, String> fparams,
                              String method,
                              String body) {
	    this.hdrs = hdrs;
		this.qparams = qparams;
		this.fparams = fparams;
		this.method = method;
		this.body = body;
    }



	/**
	 * For jackson json ser/deserialization
	 */
	@SuppressWarnings("unused")
	private HTTPRequestPayload() {
		super();
		this.hdrs = new MultivaluedHashMap<String, String>();
		this.qparams = new MultivaluedHashMap<String, String>();
		this.fparams = new MultivaluedHashMap<String, String>();
		this.method = "";
		this.body = "";
	}



    @JsonDeserialize(as=MultivaluedHashMap.class)
    public final MultivaluedMap<String, String> hdrs;
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> qparams; // query params
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> fparams; // form params
	public final String method;
    public final String body;




	private static MultivaluedHashMap<String, String> emptyMap () {
		return new MultivaluedHashMap<String, String>();
	}

}
