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

import com.cube.dao.Request.ReqMatchSpec;
import com.cube.drivers.Analysis;
import com.cube.drivers.Analysis.Result;
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
		ReplayMeta, // replay metadata
		Analysis,
		Result
	}

	boolean save(Request req);
	
	boolean save(Response resp);
		
	/**
	 * @param queryrequest
	 * @param mspec - the matching specification
	 * @param nummatches TODO
	 * @return the requests matching queryrequest based on the matching spec
	 */
	List<Request> getRequests(Request queryrequest, ReqMatchSpec mspec, Optional<Integer> nummatches);
	
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
	Optional<Response> getRespForReq(Request queryrequest, ReqMatchSpec mspec);


	/**
	 * @param customerid
	 * @param app
	 * @param collection
	 * @param reqids
	 * @param paths 
	 * @param rrtype
	 * @return
	 */
	List<Request> getRequests(String customerid, String app, String collection, List<String> reqids, List<String> paths, RRBase.RR rrtype);

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
	 * @param analysis
	 * @return
	 */
	boolean saveAnalysis(Analysis analysis);

	/**
	 * @param res
	 * @return 
	 */
	boolean saveResult(Result res);

	/**
	 * @param replayid
	 * @return
	 */
	Optional<Analysis> getAnalysis(String replayid);

	
}
