/*
 *
 *    Copyright Cube I O
 *
 */
package com.cube.dao;

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
		
	public enum RespMatchType {
		ExactMatch,
		TemplateMatch,
		NoMatch;

		/**
		 * @param other
		 * @return
		 */
		public boolean isBetter(RespMatchType other) {
			switch (this) {
				case NoMatch: return (other == NoMatch); // NOTE: NoMatch is considered better than NoMatch to handle the starting condition
				case ExactMatch: return (other != ExactMatch);
				default: return (other == NoMatch); // PartialMatch is better only if other is NoMatch
			}
		}
	}


/*
	public static class RespMatch {
		
		public RespMatch(RespMatchType respmt, String respmatchmetadata) {
			super();
			this.respmt = respmt;
			this.respmatchmetadata = respmatchmetadata;
		}
		
		final MatchType respmt;
		final String respmatchmetadata;		
	}
*/

	public static class RespMatchWithReq  {

		/**
		 * @param recordreq
		 * @param replayreq
		 * @param match
		 */
		public RespMatchWithReq(Request recordreq, Request replayreq, Comparator.Match match) {
			this.recordreq = recordreq;
			this.replayreq = replayreq;
			this.match = match;
		}

		final Comparator.Match match;
		final Request recordreq;
		final Request replayreq;

		public Comparator.MatchType getmt() {
			return match.mt;
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
		private ReqRespMatchResult(String recordreqid, String replayreqid, ReqMatchType reqmt, int nummatch,
								   Comparator.Match match,
								   String customerid, String app,
								   String service, String path, String replayid, ObjectMapper jsonmapper) {
			super();
			this.recordreqid = recordreqid;
			this.replayreqid = replayreqid;
			this.reqmt = reqmt;
			this.nummatch = nummatch;
			this.respmt = match.mt;
			this.respmatchmetadata = match.matchmeta;
			this.diff = match.getDiffAsJsonStr(jsonmapper);
			this.customerid = customerid;
			this.app = app;
			this.service = service;
			this.path = path;
			this.replayid = replayid;
		}
		
		/**
		 * @param rm
		 * @param size
		 * @param replayid
		 * @param jsonmapper
		 */
		public ReqRespMatchResult(RespMatchWithReq rm, ReqMatchType reqmt, int size, String replayid, ObjectMapper jsonmapper) {
			this(rm.recordreq.reqid.orElse(""), rm.replayreq.reqid.orElse(""), reqmt, size, 
					rm.match,
					rm.recordreq.customerid.orElse(""), rm.recordreq.app.orElse(""),
					rm.recordreq.getService().orElse(""), rm.recordreq.path,
					replayid, jsonmapper);
		}

		final public String recordreqid;
		final public String replayreqid;
		final public ReqMatchType reqmt;
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
