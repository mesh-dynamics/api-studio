/*
 *
 *    Copyright Cube I O
 *
 */
package com.cube.dao;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cube.core.Comparator;
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



    public static class ReqRespMatchWithEvent {

        /**
         * @param recordReq
         * @param replayReq
         * @param respMatch
         */
        public ReqRespMatchWithEvent(Event recordReq, Optional<Event> replayReq
	        , Comparator.Match respMatch, Optional<Event> recordResp, Optional<Event> replayResp
	        , Optional<Comparator.Match> reqCompareRes) {
	        this.recordReq = recordReq;
	        this.replayReq = replayReq;
	        this.recordResp = recordResp;
	        this.replayResp = replayResp;
	        this.respMatch = respMatch;
	        this.reqCompareRes = reqCompareRes;
        }

        final Comparator.Match respMatch;
        final Optional<Comparator.Match> reqCompareRes;
        final Event recordReq;
        final Optional<Event> replayReq;
        final Optional<Event> recordResp;
        final Optional<Event> replayResp;

        public Comparator.MatchType getRespMt() {
            return respMatch.mt;
        }

        public List<Comparator.Diff> getRespDiffs() {
            return respMatch.diffs;
        }

        public Optional<Comparator.Match> getReqCompResult() {
        	return reqCompareRes;
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


}
