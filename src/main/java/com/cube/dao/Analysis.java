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
import com.cube.dao.DataObj.DataObjCreationException;
import com.cube.ws.Config;


/**
 * @author prasad
 *
 */
public class Analysis {

    private static final Logger LOGGER = LogManager.getLogger(Analysis.class);

	public enum Status {
		Running,
        MatchingCompleted,
		Completed,
		Error
	}


	/**
	 * @param replayId
	 */
	public Analysis(String replayId, int reqcnt, String templateVersion) {
		this.replayId = replayId;
		this.status = Status.Running;
		this.reqcnt = reqcnt;
		this.timestamp = System.currentTimeMillis();
		this.templateVersion = templateVersion;
	}



	/**
	 * This constructor is only for jackson json deserialization
     * @param replayId
     * @param reqcnt
     */

	private Analysis(String replayId, int reqcnt) {
		super();
		this.replayId = "";
		// Assuming this value will be overwritten during json deserialization
		this.timestamp = System.currentTimeMillis();
		this.templateVersion = "";
	}


	public final String replayId;
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
    public final long timestamp;
	/*
	 * invariants:
	 * reqanalyzed = reqmatched + reqpartiallymatched  + reqnotmatched
	 * reqanalyzed = reqsinglematch + reqmultiplematch + reqnotmatched
	 * reqanalyzed - reqnotmatched = respmatched + resppartiallymatched + respnotmatched
	 *
	 */
	public int reqanalyzed;
    public final String templateVersion;



    public static class RespMatchWithReqEvent  {

        /**
         * @param recordReq
         * @param replayReq
         * @param match
         */
        public RespMatchWithReqEvent(Event recordReq, Optional<Event> replayReq, Comparator.Match match,
                                     Optional<Event> recordResp , Optional<Event> replayResp) {
            this.recordReq = recordReq;
            this.replayReq = replayReq;
            this.recordResp = recordResp;
            this.replayResp = replayResp;
            this.match = match;
        }

        final Comparator.Match match;
        final Event recordReq;
        final Optional<Event> replayReq;
        final Optional<Event> recordResp;
        final Optional<Event> replayResp;

        public Comparator.MatchType getmt() {
            return match.mt;
        }

        public List<Comparator.Diff> getDiffs() {
            return match.diffs;
        }

        public Optional<String> getRecordedResponseBody(Config config) {
            return recordResp.map(response-> response.getPayloadAsString(config));
        }

        public Optional<String> getReplayResponseBody(Config config) {
            return replayResp.map(response -> response.getPayloadAsString(config));
        }

        public Optional<String> getReplayReq(Config config) {
            return replayReq.map(request -> request.getPayloadAsString(config));
        }


        public Optional<String> getRecordReq(Config config) {
	        return Optional.of(recordReq.getPayloadAsString(config));
        }
    }


    public static class  ReqRespMatchResult {



		/**
		 * @param recordReqId
		 * @param replayreqid
		 * @param reqmt
		 * @param match
		 * @param customerId
		 * @param app
		 * @param service
		 * @param path
		 * @param replayId
		 * @param jsonMapper
		 */
		private ReqRespMatchResult(Optional<String> recordReqId, Optional<String> replayreqid, Comparator.MatchType reqmt, int nummatch,
								   Comparator.Match match,
								   String customerId, String app,
								   String service, String path, String replayId, ObjectMapper jsonMapper,
                                   Optional<String> recordTraceId, Optional<String> replayTraceId) {
			this(recordReqId, replayreqid, reqmt, nummatch, match.mt, match.matchmeta,
					match.getDiffAsJsonStr(jsonMapper), customerId, app, service, path, replayId, recordTraceId, replayTraceId);
		}

		/**
		 *
		 * @param recordReqId
		 * @param replayReqId
		 * @param reqMt
		 * @param numMatch
		 * @param respMt
		 * @param matchMetaData
		 * @param diff
		 * @param customerId
		 * @param app
		 * @param service
		 * @param path
		 * @param replayId
		 */
		public ReqRespMatchResult(Optional<String> recordReqId, Optional<String> replayReqId, Comparator.MatchType reqMt, int numMatch,
								   Comparator.MatchType respMt, String matchMetaData, String diff,
								   String customerId, String app,
								   String service, String path, String replayId,
                                   Optional<String> recordTraceId, Optional<String> replayTraceId) {
			super();
			this.recordReqId = recordReqId;
			this.replayReqId = replayReqId;
			this.reqMatchType = reqMt;
			this.numMatch = numMatch;
			this.respMatchType = respMt;
			this.respMatchMetadata = matchMetaData;
			this.diff = diff;
			this.customerId = customerId;
			this.app = app;
			this.service = service;
			this.path = path;
			this.replayId = replayId;
			this.recordTraceId = recordTraceId;
			this.replayTraceId = replayTraceId;
		}


        public ReqRespMatchResult(RespMatchWithReqEvent rm, Comparator.MatchType reqmt, int size, String replayId,
                                  ObjectMapper jsonmapper) {
            this(Optional.of(rm.recordReq.reqId), rm.replayReq.map(req -> req.reqId), reqmt, size,
                rm.match,
                rm.recordReq.customerId, rm.recordReq.app,
                rm.recordReq.service, rm.recordReq.apiPath,
                replayId, jsonmapper, Optional.of(rm.recordReq.traceId),
                rm.replayReq.map(replayreq -> replayreq.traceId)) ;
        }

        final public Optional<String> recordReqId;
        final public Optional<String> replayReqId;
		final public Optional<String> recordTraceId;
		final public Optional<String> replayTraceId;
		final public Comparator.MatchType reqMatchType;
		final public int numMatch;
		final public Comparator.MatchType respMatchType;
		final public String respMatchMetadata;
		final public String diff;
		final public String customerId;
		final public String app;
		final public String service;
		final public String path;
		final public String replayId;
	}

}
