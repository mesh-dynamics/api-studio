/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

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
		
		
		
		/**
		 * @param path
		 * @param qparams
		 * @param fparams
		 */
		public Request(String path, MultivaluedMap<String, String> qparams, MultivaluedMap<String, String> fparams) {
			this(path, Optional.empty(), qparams, fparams, new MultivaluedHashMap<String, String>(), 
					new MultivaluedHashMap<String, String>(), new MultivaluedHashMap<String, String>(), "");
		}

		
		
		/**
		 * 
		 */
		@SuppressWarnings("unused")
		private Request() {
			super();
			this.path = ""; 
			this.id = Optional.empty();
			this.qparams = new MultivaluedHashMap<String, String>();
			this.fparams = new MultivaluedHashMap<String, String>();
			this.meta = new MultivaluedHashMap<String, String>();
			this.hdrs = new MultivaluedHashMap<String, String>();
			this.cookies = new MultivaluedHashMap<String, String>();
			this.body = "";
		}



		static final TypeReference<MultivaluedHashMap<String, String>> typeRef 
		  = new TypeReference<MultivaluedHashMap<String, String>>() {};
		
		public final String path;
		public final Optional<String> id;
        @JsonDeserialize(using=MultivaluedMapDeserializer.class)
        //@JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> qparams; // query params
        @JsonDeserialize(using=MultivaluedMapDeserializer.class)
        //@JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> fparams; // form params
        @JsonDeserialize(using=MultivaluedMapDeserializer.class)
        //@JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> meta; 
        @JsonDeserialize(using=MultivaluedMapDeserializer.class)
        //@JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> hdrs;
        @JsonDeserialize(using=MultivaluedMapDeserializer.class)
        //@JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> cookies;
		public final String body;
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
		
		/**
		 * 
		 */
		@SuppressWarnings("unused")
		private Response() {
			super();
			this.reqid = Optional.empty();
			this.status = Status.OK.getStatusCode();
			this.meta = new MultivaluedHashMap<String, String>();
			this.hdrs = new MultivaluedHashMap<String, String>();
			this.body = "";
		}

		
		public final Optional<String> reqid;
		public final int status;
        @JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> meta; 
        @JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> hdrs;
		public final String body;		
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
