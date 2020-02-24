/*
 *
 *    Copyright Cube I O
 *
 */
package io.md.dao;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;

import io.md.utils.Utils;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-10-01
 */
public class HTTPRequestPayload extends AbstractMDPayload {

    /**
     *
     * @param hdrs
     * @param queryParams
     * @param formParams
     * @param method
     * @param body
     */
	public HTTPRequestPayload(MultivaluedMap<String, String> hdrs,
                              MultivaluedMap<String, String> queryParams,
                              MultivaluedMap<String, String> formParams,
                              String method,
                              byte[] body) {
	    this.hdrs = Utils.setLowerCaseKeys(hdrs);
		this.queryParams = queryParams;
		this.formParams = formParams;
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
		this.queryParams = new MultivaluedHashMap<String, String>();
		this.formParams = new MultivaluedHashMap<String, String>();
		this.method = "";
		this.body = new byte[]{};
	}



    @JsonDeserialize(as=MultivaluedHashMap.class)
    public final MultivaluedMap<String, String> hdrs;
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> queryParams; // query params
    @JsonDeserialize(as=MultivaluedHashMap.class)
	public final MultivaluedMap<String, String> formParams; // form params
	public final String method;
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
    public final byte[] body;




	private static MultivaluedHashMap<String, String> emptyMap () {
		return new MultivaluedHashMap<String, String>();
	}

}
