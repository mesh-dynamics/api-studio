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
import com.cube.dao.RRBase.RR;
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

		// optional matching on traceid //and requestid
		ReqMatchSpec mspec = (ReqMatchSpec) ReqMatchSpec.builder()
				.withMpath(MatchType.FILTER)
				.withMqparams(MatchType.FILTER)
				.withMfparams(MatchType.FILTER)
				.withMhdrs(MatchType.SCORE)
				.withHdrfields(Collections.singletonList(tracefield))
				.withMrrtype(MatchType.FILTER)
				.withMcustomerid(MatchType.FILTER)
				.withMapp(MatchType.FILTER).build();
				//.withMreqid(MatchType.SCORE).build();

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
		reqs.forEach(r -> {
			// find matching request in replay
			// same fields as request, only RRType should be Replay
			Request rq = new Request(r.path, r.reqid, r.qparams, r.fparams, r.meta, 
					r.hdrs, r.method, r.body, r.collection, r.timestamp, 
					Optional.of(RR.Replay.toString()), r.customerid, r.app);
			List<Request> matches = rrstore.getRequests(rq, mspec, Optional.ofNullable(10));
			
			matches.stream().findFirst().ifPresentOrElse(firstmatch -> {
				if (matches.size() > 1) {
					reqmultiplematch++;
				}
				else {
					reqsinglematch++;
				}

				// fetch response of recording and replay

				RespMatchWithReq bestmatch = new RespMatchWithReq(r, null, RespMatchType.NoMatch, "");
				ReqMatchType reqmt = rq.compare(firstmatch, mspec);

				// matches is ordered in decreasing order of request match score. so exact matches
				// of requests, if any should be at the beginning
				// If request matches exactly, consider that as the best match
				// else find the best match based on response matching
				if (reqmt == ReqMatchType.ExactMatch) {
					bestmatch = checkRespMatch(r, firstmatch, rrstore);
				} else {
					for (Request replayreq : matches) {						
						RespMatchWithReq match = checkRespMatch(r, replayreq, rrstore);
						if (match.respmt == RespMatchType.ExactMatch) {
							bestmatch = match;
							break;
						} else if (bestmatch.respmt == RespMatchType.NoMatch) {
							bestmatch = match;
						}
					}					
					reqmt = rq.compare(bestmatch.replayreq, mspec);
				}
				// compare & write out result
				if (reqmt == ReqMatchType.ExactMatch)
					reqmatched++;
				else
					reqpartiallymatched++;
				switch(bestmatch.respmt) {
					case ExactMatch: respmatched++; break;
					case TemplateMatch: resppartiallymatched++; break;
					default: respnotmatched++; break;
				}
				Result res = new Result(bestmatch, reqmt, matches.size(), replayid);						
				rrstore.saveResult(res);
				
			}, () -> reqnotmatched++);
			reqanalyzed++;
			if (reqanalyzed % UPDBATCHSIZE == 0) {
				LOGGER.info(String.format("Analysis of replay %s completed %d requests", replayid, reqanalyzed));
				rrstore.saveAnalysis(this);
			}
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

	private RespMatchWithReq checkRespMatch(Request recordreq, Request replayreq, ReqRespStore rrstore) {
		return recordreq.reqid.flatMap(recordreqid -> {
			return replayreq.reqid.flatMap(replayreqid -> {
				// fetch response of recording and replay
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
		NoMatch;
		
		public ReqMatchType And(ReqMatchType other) {
			switch (this) {
			case NoMatch: return NoMatch;
			case ExactMatch: return other;
			default: return (other == NoMatch) ? NoMatch : PartialMatch;
			}
		}
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
		 * @param customerid 
		 * @param app 
		 * @param service 
		 * @param path
		 * @param replayid 
		 */
		private Result(String recordreqid, String replayreqid, ReqMatchType reqmt, int nummatch, 
				RespMatchType respmt,
				String respmatchmetadata, String customerid, String app, 
				String service, String path, String replayid) {
			super();
			this.recordreqid = recordreqid;
			this.replayreqid = replayreqid;
			this.reqmt = reqmt;
			this.nummatch = nummatch;
			this.respmt = respmt;
			this.respmatchmetadata = respmatchmetadata;
			this.customerid = customerid;
			this.app = app;
			this.service = service;
			this.path = path;
			this.replayid = replayid;
		}
		
		/**
		 * @param bestmatch
		 * @param size
		 * @param replayid 
		 */
		public Result(RespMatchWithReq rm, ReqMatchType reqmt, int size, String replayid) {
			this(rm.recordreq.reqid.orElse(""), rm.replayreq.reqid.orElse(""), reqmt, size, 
					rm.respmt, rm.respmatchmetadata,
					rm.recordreq.customerid.orElse(""), rm.recordreq.app.orElse(""),
					rm.recordreq.getService().orElse(""), rm.recordreq.path,
					replayid);
		}

		final public String recordreqid;
		final public String replayreqid;
		final public ReqMatchType reqmt;
		final public int nummatch;
		final public RespMatchType respmt;
		final public String respmatchmetadata;
		final public String customerid;
		final public String app;
		final public String service;
		final public String path;
		final public String replayid;
	}

}
