/*
 *
 *    Copyright Cube I O
 *
 */
package com.cube.dao;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.core.Comparator;


/**
 * @author prasad
 *
 */
public class Analysis {

    private static final Logger LOGGER = LogManager.getLogger(Analysis.class);

	public enum Status {
		Running,
		Completed,
		Error
	}
	
		
	/**
	 * @param replayid
	 */
	public Analysis(String replayid, int reqcnt) {
		this.replayid = replayid;
		this.status = Status.Running;
		this.reqcnt = reqcnt;
	}



	/**
	 * This constructor is only for jackson json deserialization
	 */
	
	private Analysis() {
		super();
		this.replayid = "";
	}


	public final String replayid;
	public Status status;
	public int reqcnt=0; // total number of requests
	public int reqmatched=0; // number of requests exactly matched
	public int reqpartiallymatched=0; // number of requests partially matched
	public int reqsinglematch=0; // matched with only one request in replay 
	public int reqmultiplematch=0; // matched multiple requests in the replay
	public int reqnotmatched=0; // not matched 
	public int respmatched=0; // resp matched exactly
	public int resppartiallymatched=0; // resp matched based on template
	public int respnotmatched=0; // not matched
    public long timestamp;
	/*
	 * invariants:
	 * reqanalyzed = reqmatched + reqpartiallymatched  + reqnotmatched
	 * reqanalyzed = reqsinglematch + reqmultiplematch + reqnotmatched
	 * reqanalyzed - reqnotmatched = respmatched + resppartiallymatched + respnotmatched
	 * 
	 */
	public int reqanalyzed;


	public enum ReqMatchType {
		ExactMatch,
		PartialMatch,
		NoMatch;
		
		public ReqMatchType And(ReqMatchType other) {
			switch (this) {
				case NoMatch: return NoMatch;
				case ExactMatch: return other;
				default: return (other == NoMatch) ? NoMatch : PartialMatch;
			}
		}

		/**
		 * @param other
		 * @return true if this is better or equal to other match
		 */
		public boolean isBetterOrEqual(ReqMatchType other) {
			switch (this) {
				case NoMatch: return (other == NoMatch);
				case ExactMatch: return true;
				default: return (other != ExactMatch); // PartialMatch is better only if other is not ExactMatch
			}
		}
	}



	public static class RespMatchWithReq  {

		/**
		 * @param recordreq
		 * @param replayreq
		 * @param match
		 */
		public RespMatchWithReq(Request recordreq, Optional<Request> replayreq, Comparator.Match match,
								Optional<Response> recordres , Optional<Response> replayres) {
			this.recordreq = recordreq;
			this.replayreq = replayreq;
			this.recordres = recordres;
			this.replayres = replayres;
			this.match = match;
		}

		final Comparator.Match match;
		final Request recordreq;
		final Optional<Request> replayreq;
		final Optional<Response> recordres;
		final Optional<Response> replayres;

		public Comparator.MatchType getmt() {
			return match.mt;
		}

		public List<Comparator.Diff> getDiffs() {
			return match.diffs;
		}

		public Optional<String> getRecordedResponseBody() {
			return recordres.map(response -> response.body);
		}

		public Optional<String> getReplayResponseBody() {
			return replayres.map(response -> response.body);
		}

		private Optional<String> serializeRequest(Request request, ObjectMapper jsonMapper) {
			try {
				return Optional.of(jsonMapper.writeValueAsString(request));
			} catch (Exception e) {
				return Optional.empty();
			}
		}

		public Optional<String> getReplayReq(ObjectMapper jsonMapper) {
			return replayreq.flatMap(request -> serializeRequest(request , jsonMapper));
		}


		public Optional<String> getRecordReq(ObjectMapper jsonMapper) {
			return serializeRequest(recordreq , jsonMapper);
		}
	}

	
	public static class ReqRespMatchResult {
		
		
		
		/**
		 * @param recordreqid
		 * @param replayreqid
		 * @param reqmt
		 * @param match
		 * @param customerid
		 * @param app
		 * @param service
		 * @param path
		 * @param replayid
		 * @param jsonmapper
		 */
		private ReqRespMatchResult(String recordreqid, String replayreqid, Comparator.MatchType reqmt, int nummatch,
								   Comparator.Match match,
								   String customerid, String app,
								   String service, String path, String replayid, ObjectMapper jsonmapper) {
			this(recordreqid, replayreqid, reqmt, nummatch, match.mt, match.matchmeta,
					match.getDiffAsJsonStr(jsonmapper), customerid, app, service, path, replayid);
		}

		/**
		 *
		 * @param recordreqid
		 * @param replayreqid
		 * @param reqMt
		 * @param nummatch
		 * @param respMt
		 * @param matchMetaData
		 * @param diff
		 * @param customerid
		 * @param app
		 * @param service
		 * @param path
		 * @param replayid
		 */
		public ReqRespMatchResult(String recordreqid, String replayreqid, Comparator.MatchType reqMt, int nummatch,
								   Comparator.MatchType respMt, String matchMetaData, String diff,
								   String customerid, String app,
								   String service, String path, String replayid) {
			super();
			this.recordreqid = recordreqid;
			this.replayreqid = replayreqid;
			this.reqmt = reqMt;
			this.nummatch = nummatch;
			this.respmt = respMt;
			this.respmatchmetadata = matchMetaData;
			this.diff = diff;
			this.customerid = customerid;
			this.app = app;
			this.service = service;
			this.path = path;
			this.replayid = replayid;
		}

		/**
		 * @param rm
		 * @param reqmt
		 * @param size
		 * @param replayid
		 * @param jsonmapper
		 */
		public ReqRespMatchResult(RespMatchWithReq rm, Comparator.MatchType reqmt, int size, String replayid, ObjectMapper jsonmapper) {
			this(rm.recordreq.reqid.orElse(""), rm.replayreq.flatMap(req -> req.reqid).orElse(""), reqmt, size,
					rm.match,
					rm.recordreq.customerid.orElse(""), rm.recordreq.app.orElse(""),
					rm.recordreq.getService().orElse(""), rm.recordreq.path,
					replayid, jsonmapper);
		}

		final public String recordreqid;
		final public String replayreqid;
		final public Comparator.MatchType reqmt;
		final public int nummatch;
		final public Comparator.MatchType respmt;
		final public String respmatchmetadata;
		final public String diff;
		final public String customerid;
		final public String app;
		final public String service;
		final public String path;
		final public String replayid;
	}
	
}
