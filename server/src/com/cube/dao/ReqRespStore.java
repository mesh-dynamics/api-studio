/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import com.cube.drivers.Replay;
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

	enum Types {
		Request,
		Response,
		ReplayMeta // replay metadata
	}

	enum RR {
		Record,
		Replay
	}
	
	public class RRBase {

		/**
		 * @param path
		 * @param id
		 * @param qparams
		 * @param meta
		 * @param hdrs
		 * @param body
		 */
		public RRBase(Optional<String> reqid, 
				MultivaluedMap<String, String> meta, 
				MultivaluedMap<String, String> hdrs, 
				String body,
				Optional<String> collection,
				Optional<Instant> timestamp, 
				Optional<String> rrtype, 
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
		 * 
		 */
		@SuppressWarnings("unused")
		private RRBase() {
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

		public final Optional<String> reqid;
        @JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> meta; 
        @JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> hdrs;
		public final String body;		
		public final Optional<String> collection;
		public final Optional<Instant> timestamp;
		public final Optional<String> rrtype; // this can be "record" or "replay"
		public final Optional<String> customerid;
		public final Optional<String> app;
	}
	
	public class Request extends RRBase {
		/**
		 * @param path
		 * @param id
		 * @param qparams
		 * @param meta
		 * @param hdrs
		 * @param body
		 */
		public Request(String path, Optional<String> reqid, MultivaluedMap<String, String> qparams,
				MultivaluedMap<String, String> fparams,
				MultivaluedMap<String, String> meta, 
				MultivaluedMap<String, String> hdrs, 
				String method, 
				String body,
				Optional<String> collection,
				Optional<Instant> timestamp, 
				Optional<String> rrtype, 
				Optional<String> customerid,
				Optional<String> app) {
			super(reqid, meta, hdrs, body, collection, timestamp, rrtype, customerid, app);
			this.path = path; 
			this.qparams = qparams != null ? qparams : new MultivaluedHashMap<String, String>();
			this.fparams = fparams != null ? fparams : new MultivaluedHashMap<String, String>();
			this.method = method;
		}
		
		
		
		/**
		 * @param path
		 * @param qparams
		 * @param fparams
		 */
		public Request(String path, Optional<String> id, 
				MultivaluedMap<String, String> qparams, 
				MultivaluedMap<String, String> fparams, 
				Optional<String> collection, 
				Optional<String> rrtype, 
				Optional<String> customerid,
				Optional<String> app) {
			this(path, id, qparams, fparams, new MultivaluedHashMap<String, String>(), 
					new MultivaluedHashMap<String, String>(), "", "", collection, Optional.empty(), rrtype, customerid, app);
		}

		
		
		/**
		 * 
		 */
		@SuppressWarnings("unused")
		private Request() {
			super();
			this.path = ""; 
			this.qparams = new MultivaluedHashMap<String, String>();
			this.fparams = new MultivaluedHashMap<String, String>();
			this.method = "";
		}



		static final TypeReference<MultivaluedHashMap<String, String>> typeRef 
		  = new TypeReference<MultivaluedHashMap<String, String>>() {};
		
		public final String path;
        @JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> qparams; // query params
        @JsonDeserialize(as=MultivaluedHashMap.class)
		public final MultivaluedMap<String, String> fparams; // form params
		public final String method;
	}
	
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
				Optional<String> rrtype, 
				Optional<String> customerid,
				Optional<String> app) {
			super(reqid, meta, hdrs, body, collection, timestamp, rrtype, customerid, app);
			this.status = status;
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

	/**
	 * @param customerid
	 * @param app
	 * @param collection
	 * @param reqids
	 * @param paths 
	 * @param rrtype
	 * @return
	 */
	List<Request> getRequests(String customerid, String app, String collection, List<String> reqids, List<String> paths, RR rrtype);

	/**
	 * @param replay
	 * @return 
	 */
	boolean saveReplay(Replay replay);

	/**
	 * @param replayid
	 * @return
	 */
	Optional<Replay> getReplay(String replayid);
	
}
