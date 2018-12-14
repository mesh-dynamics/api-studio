/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @author prasad
 *
 */
public interface ReqRespStore {
	
	
	public class ReqResp {
				
		
		/**
		 * @param pathwparams
		 * @param meta
		 * @param hdrs
		 * @param body
		 */
		private ReqResp(String pathwparams, List<Map.Entry<String, String>> meta,
				List<Map.Entry<String, String>> hdrs, String body) {
			super();
			this.pathwparams = pathwparams;
			this.meta = meta;
			this.hdrs = hdrs;
			this.body = body;
		}
		
		
		
		/**
		 * 
		 */
		private ReqResp() {
			super();
			this.pathwparams = "";
			this.meta = new ArrayList<Map.Entry<String, String>>();
			this.hdrs = new ArrayList<Map.Entry<String, String>>();
			this.body = "";
		}


		@JsonProperty("path")
		public final String pathwparams; // path with params
        @JsonDeserialize(as=ArrayList.class)
		public final List<Map.Entry<String, String>> meta;
        @JsonDeserialize(as=ArrayList.class)
		public final List<Map.Entry<String, String>> hdrs;
		public final String body;
		
	}
	
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
				String method, 
				String body) {
			super();
			this.path = path; 
			this.id = id;
			this.qparams = qparams != null ? qparams : new MultivaluedHashMap<String, String>();
			this.fparams = fparams != null ? fparams : new MultivaluedHashMap<String, String>();
			this.meta = meta != null ? meta : new MultivaluedHashMap<String, String>();
			this.hdrs = hdrs != null ? hdrs : new MultivaluedHashMap<String, String>();
			this.method = method;
			this.body = body;
		}
		
		
		
		/**
		 * @param path
		 * @param qparams
		 * @param fparams
		 */
		public Request(String path, MultivaluedMap<String, String> qparams, MultivaluedMap<String, String> fparams) {
			this(path, Optional.empty(), qparams, fparams, new MultivaluedHashMap<String, String>(), 
					new MultivaluedHashMap<String, String>(), "", "");
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
			this.method = "";
			this.body = "";
		}



		static final TypeReference<MultivaluedHashMap<String, String>> typeRef 
		  = new TypeReference<MultivaluedHashMap<String, String>>() {};
		
		public final String path;
		public final Optional<String> id;
        @JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> qparams; // query params
        @JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> fparams; // form params
        @JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> meta; 
        @JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> hdrs;
		public final String method;
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

	static void main(String[] args) throws IOException{

		Map.Entry<String, String> e = new AbstractMap.SimpleEntry<String, String>("k1", "v1");
		ObjectMapper m1 = new ObjectMapper();
		String jr = m1.writerWithDefaultPrettyPrinter()
				  .writeValueAsString(e);

		System.out.println(String.format("Json string: %s", jr));
		
		TypeReference<Map.Entry<String, String>> tR 
		  = new TypeReference<Map.Entry<String, String>>() {};
		Map.Entry<String, String> e2 = m1.readValue(jr, tR);
		System.out.println("Object read back: " + e2.toString());
		
		
		List<Map.Entry<String, String>> meta = new ArrayList<AbstractMap.Entry<String, String>>();
		meta.add(new SimpleEntry<String, String>("m1", "m1v1"));
		meta.add(new SimpleEntry<String, String>("m1", "m1v2"));
		meta.add(new SimpleEntry<String, String>("m2", "m2v1"));
		meta.add(new SimpleEntry<String, String>("m2", "m2v1"));
		
		List<Map.Entry<String, String>> hdrs = new ArrayList<AbstractMap.Entry<String, String>>();
		hdrs.add(new SimpleEntry<String, String>("h1", "h1v1"));
		hdrs.add(new SimpleEntry<String, String>("h1", "h1v2"));
		hdrs.add(new SimpleEntry<String, String>("h2", "h2v1"));
		hdrs.add(new SimpleEntry<String, String>("h2", "h2v1"));
		
		ReqResp rr = new ReqResp("/p1?a=av", meta, hdrs, "body 1");
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonResult = mapper.writerWithDefaultPrettyPrinter()
		  .writeValueAsString(rr);

		System.out.println(String.format("Json string: %s", jsonResult));
		
		TypeReference<ReqResp> typeRef 
		  = new TypeReference<ReqResp>() {};
		ReqResp rr2 = mapper.readValue(jsonResult, typeRef);
		System.out.println("Object read back: " + rr2.toString());

		String jsonResult2 = mapper.writerWithDefaultPrettyPrinter()
				  .writeValueAsString(rr2);

		System.out.println(String.format("Json string: %s", jsonResult2));

		
	}
	
}
