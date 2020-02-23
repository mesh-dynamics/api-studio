/*
 *
 *    Copyright Cube I O
 *
 */
package io.md.dao;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;
import io.md.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

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
                               byte[] body) {
	    this.hdrs = Utils.setLowerCaseKeys(hdrs);
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
		this.body = new byte[]{0};
	}


    @JsonDeserialize(as=MultivaluedHashMap.class)
    public final MultivaluedMap<String, String> hdrs;
	public final int status;
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
    public final byte[] body;

	private static MultivaluedHashMap<String, String> emptyMap () {
		return new MultivaluedHashMap<String, String>();
	}

}
