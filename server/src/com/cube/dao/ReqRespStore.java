/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.util.Optional;

import javax.ws.rs.core.MultivaluedMap;

/**
 * @author prasad
 *
 */
public interface ReqRespStore {
	
	public class Request {
		/**
		 * @param path
		 * @param id
		 * @param qparams
		 * @param meta
		 * @param hdrs
		 * @param body
		 */
		public Request(String path, Optional<String> id, MultivaluedMap<String, String> qparams,
				MultivaluedMap<String, String> fparams,
				MultivaluedMap<String, String> meta, 
				MultivaluedMap<String, String> hdrs, 
				MultivaluedMap<String, String> cookies, 
				String body) {
			super();
			this.path = path; 
			this.id = id;
			this.qparams = qparams;
			this.fparams = fparams;
			this.meta = meta;
			this.hdrs = hdrs;
			this.cookies = cookies;
			this.body = body;
		}
		final String path;
		final Optional<String> id;
		final MultivaluedMap<String, String> qparams; // query params
		final MultivaluedMap<String, String> fparams; // form params
		final MultivaluedMap<String, String> meta; 
		final MultivaluedMap<String, String> hdrs;
		final MultivaluedMap<String, String> cookies;
		final String body;
	}
	
	public class Response {
		/**
		 * @param reqid
		 * @param status
		 * @param hdrs
		 * @param body
		 */
		public Response(Optional<String> reqid, int status, 
				MultivaluedMap<String, String> meta, 
				MultivaluedMap<String, String> hdrs, String body) {
			super();
			this.reqid = reqid;
			this.status = status;
			this.meta = meta;
			this.hdrs = hdrs;
			this.body = body;
		}
		final Optional<String> reqid;
		final int status;
		final MultivaluedMap<String, String> meta; 
		final MultivaluedMap<String, String> hdrs;
		final String body;		
	}
	
	boolean save(Request req);
	
	boolean save(Response resp);
		
	/**
	 * @param queryrequest
	 * @param ignoreId - whether id should be ignore in the matching
	 * @return the matching request based on matching path, id (optional based on ignoreId flag), qparams and fparams
	 * hdrs and cookies are ignored
	 */
	Optional<Request> getRequest(Request queryrequest, boolean ignoreId);
	
	/**
	 * @param reqid
	 * @return the matching response on the reqid 
	 */
	Optional<Response> getResponse(String reqid);
	
	/**
	 * @param queryrequest
	 * @return the response corresponding to the request matching in the db
	 * to find the matching request, the reqid field of queryrequest is ignored
	 */
	Optional<Response> getRespForReq(Request queryrequest);

}
