/**
 * Copyright Cube I O
 */
package com.cube.drivers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cube.core.ResponseComparator;
import com.cube.dao.RRBase.RRMatchSpec.MatchType;
import com.cube.dao.ReqRespStore;
import com.cube.dao.Request;
import com.cube.dao.Request.ReqMatchSpec;
import com.cube.dao.Response;
import com.fasterxml.jackson.annotation.JsonIgnore;


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
	private Analysis(String replayid, int reqcnt) {
		this.replayid = replayid;
		this.status = Status.Running;
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
	@JsonIgnore
	private ResponseComparator comparator = ResponseComparator.EQUALITYCOMPARATOR;
	

	/**
	 * @param replayid
	 * @param tracefield
	 * @return
	 */
	public static Optional<Analysis> analyze(String replayid, String tracefield, 
			ReqRespStore rrstore) {
		// String collection = Replay.getCollectionFromReplayId(replayid);
		
		Optional<Replay> replay = rrstore.getReplay(replayid);

		// optional matching on traceid and requestid
		ReqMatchSpec mspec = (ReqMatchSpec) ReqMatchSpec.builder()
				.withMpath(MatchType.FILTER)
				.withMqparams(MatchType.FILTER)
				.withMfparams(MatchType.FILTER)
				.withMhdrs(MatchType.SCORE)
				.withHdrfields(Collections.singletonList(tracefield))
				.withMrrtype(MatchType.FILTER)
				.withMcustomerid(MatchType.FILTER)
				.withMapp(MatchType.FILTER)
				.withMreqid(MatchType.SCORE).build();

		return replay.flatMap(r -> {
			List<Request> reqs = r.getRequests();
			Analysis analysis = new Analysis(replayid, reqs.size());
			if (!rrstore.saveAnalysis(analysis))
				return Optional.empty();
			
			analysis.analyze(rrstore, reqs.stream(), mspec);

			analysis.status = Status.Completed;
			// update the stored analysis
			rrstore.saveAnalysis(analysis);
			
			return Optional.of(analysis);
		});
	}

	/**
	 * @param rrstore
	 * @param mspec
	 * @param reqs
	 */
	private void analyze(ReqRespStore rrstore, Stream<Request> reqs, ReqMatchSpec mspec) {
		// TODO Auto-generated method stub
		
		reqs.forEach(r -> {
			// find matching request in replay
			List<Request> matches = rrstore.getRequests(r, mspec, Optional.ofNullable(10));
			
			if (matches.isEmpty()) {
				reqnotmatched++;
				return;
			}
			else {
				// fetch response of recording and replay
				if (matches.size() > 1) {
					reqmultiplematch++;
				}
				else {
					reqsinglematch++;
				}

				RespMatchWithReq bestmatch = new RespMatchWithReq(r, null, RespMatchType.NoMatch, "");
				for (Request replayreq : matches) {
					RespMatchWithReq match = checkMatch(r, replayreq, rrstore);
					if (match.respmt == RespMatchType.ExactMatch) {
						bestmatch = match;
						break;
					} else if (bestmatch.respmt == RespMatchType.NoMatch) {
						bestmatch = match;
					}
				}
				ReqMatchType reqmt = ReqMatchType.ExactMatch;
				if (bestmatch.respmt != RespMatchType.NoMatch) {
					// found match
					reqmt = r.compare(bestmatch.replayreq, mspec);
				}
				// compare & write out result
				if (reqmt == ReqMatchType.ExactMatch)
					reqmatched++;
				else
					reqpartiallymatched++;
				if (bestmatch.respmt == RespMatchType.ExactMatch)
					respmatched++;
				else
					resppartiallymatched++;
				Result res = new Result(bestmatch, reqmt, matches.size());						
				rrstore.saveResult(res);
			}
			reqanalyzed++;
			if (reqanalyzed % UPDBATCHSIZE == 0)
				LOGGER.info(String.format("Analysis of replay %s completed %d requests", replayid, reqanalyzed));
				rrstore.saveAnalysis(this);
		});		
	}



	/**
	 * @param replayid
	 * @param rrstore
	 * @return
	 */
	public static Optional<Analysis> getStatus(String replayid, ReqRespStore rrstore) {
		return rrstore.getAnalysis(replayid);
	}

	private RespMatchWithReq checkMatch(Request recordreq, Request replayreq, ReqRespStore rrstore) {
		return recordreq.reqid.flatMap(recordreqid -> {
			return replayreq.reqid.flatMap(replayreqid -> {
				Optional<Response> recordedresp = rrstore.getResponse(recordreqid);
				Optional<Response> replayresp = rrstore.getResponse(replayreqid);
				
				
				return recordedresp.flatMap(recordedr -> replayresp.flatMap(replayr -> {
					RespMatch rm = comparator.compare(recordedr, replayr);
					return Optional.ofNullable(new RespMatchWithReq(recordreq, replayreq, rm.respmt, rm.respmatchmetadata));
				}));																
			});			
		}).orElse(new RespMatchWithReq(recordreq, replayreq, RespMatchType.NoMatch, ""));
	}
	
	private static int UPDBATCHSIZE = 10;
	
	public enum ReqMatchType {
		ExactMatch,
		PartialMatch,
		NoMatch
	}
		
	public enum RespMatchType {
		ExactMatch,
		TemplateMatch,
		NoMatch
	}
	
	public static class RespMatch {
		
		/**
		 * @param respmt
		 * @param respmatchmetadata
		 */
		public RespMatch(RespMatchType respmt, String respmatchmetadata) {
			super();
			this.respmt = respmt;
			this.respmatchmetadata = respmatchmetadata;
		}
		
		final RespMatchType respmt;
		final String respmatchmetadata;		
	}

	public static class RespMatchWithReq extends RespMatch {

		/**
		 * @param recordreq
		 * @param replayreq
		 * @param respmt
		 * @param respmatchmetadata
		 */
		public RespMatchWithReq(Request recordreq, Request replayreq, RespMatchType respmt, String respmatchmetadata) {
			super(respmt, respmatchmetadata);
			this.recordreq = recordreq;
			this.replayreq = replayreq;
		}

		final Request recordreq;
		final Request replayreq;

	}
	
	public static class Result {
		
		
		
		/**
		 * @param recordreqid
		 * @param replayreqid
		 * @param reqmt
		 * @param respmt
		 * @param respmatchmetadata
		 */
		private Result(String recordreqid, String replayreqid, ReqMatchType reqmt, int nummatch, 
				RespMatchType respmt,
				String respmatchmetadata) {
			super();
			this.recordreqid = recordreqid;
			this.replayreqid = replayreqid;
			this.reqmt = reqmt;
			this.nummatch = nummatch;
			this.respmt = respmt;
			this.respmatchmetadata = respmatchmetadata;
		}
		
		/**
		 * @param bestmatch
		 * @param size
		 */
		public Result(RespMatchWithReq rm, ReqMatchType reqmt, int size) {
			this(rm.recordreq.reqid.orElse(""), rm.replayreq.reqid.orElse(""), reqmt, size, rm.respmt, rm.respmatchmetadata);
		}

		final public String recordreqid;
		final public String replayreqid;
		final ReqMatchType reqmt;
		final int nummatch;
		final RespMatchType respmt;
		final String respmatchmetadata;		
	}

}
