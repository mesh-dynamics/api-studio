/*
 *
 *    Copyright Cube I O
 *
 */
package io.md.dao;

import java.util.List;
import java.util.Optional;

import io.md.core.Comparator;


/**
 * @author prasad
 *
 */
public class Analysis {

	public enum Status {
		Running,
        MatchingCompleted,
		Completed,
		Error
	}


	/**
	 * @param replayId
     * @param replayId
     * @param reqCnt
	 */
	public Analysis(String replayId, int reqCnt, String templateVersion) {
		this.replayId = replayId;
		this.status = Status.Running;
		this.reqCnt = reqCnt;
		this.timestamp = System.currentTimeMillis();
		this.templateVersion = templateVersion;
	}



	/**
	 * This constructor is only for jackson json deserialization
     */
	private Analysis() {
		super();
		this.replayId = "";
		// Assuming this value will be overwritten during json deserialization
		this.timestamp = System.currentTimeMillis();
		this.templateVersion = "";
	}


	public final String replayId;
	public Status status;
	public int reqCnt = 0; // total number of requests
	public int reqMatched = 0; // number of requests exactly matched
	public int reqPartiallyMatched = 0; // number of requests partially matched
	public int reqSingleMatch = 0; // matched with only one request in replay
	public int reqMultipleMatch = 0; // matched multiple requests in the replay
	public int reqNotMatched = 0; // not matched
	public int reqCompareMatched = 0; // number of requests exactly matched
	public int reqComparePartiallyMatched = 0; // number of requests partially matched
	public int reqCompareNotMatched = 0; // not matched
	public int respMatched = 0; // resp matched exactly
	public int respPartiallyMatched = 0; // resp matched based on template
	public int respNotMatched = 0; // not matched
    public final long timestamp;
	/*
	 * invariants:
	 * reqanalyzed = reqmatched + reqpartiallymatched  + reqnotmatched
	 * reqanalyzed = reqsinglematch + reqmultiplematch + reqnotmatched
	 * reqanalyzed - reqnotmatched = respmatched + resppartiallymatched + respnotmatched
	 * reqanalyzed = reqCompareMatched + reqComparePartiallyMatched + reqCompareNotMatched
	 *
	 */
	public int reqAnalyzed;
    public final String templateVersion;



    public static class ReqRespMatchWithEvent {

        /**
         * @param recordReq
         * @param replayReq
         * @param respCompareRes
         */
        public ReqRespMatchWithEvent(Event recordReq, Optional<Event> replayReq
	        , Comparator.Match respCompareRes, Optional<Event> recordResp, Optional<Event> replayResp
	        , Comparator.Match reqCompareRes) {
	        this.recordReq = recordReq;
	        this.replayReq = replayReq;
	        this.recordResp = recordResp;
	        this.replayResp = replayResp;
	        this.respCompareRes = respCompareRes;
	        this.reqCompareRes = reqCompareRes;
        }

        final Comparator.Match respCompareRes;
        final Comparator.Match reqCompareRes;
        final Event recordReq;
        final Optional<Event> replayReq;
        final Optional<Event> recordResp;
        final Optional<Event> replayResp;

        public Comparator.MatchType getRespCompareResType() {
            return respCompareRes.mt;
        }

        public List<Comparator.Diff> getRespDiffs() {
            return respCompareRes.diffs;
        }

        public Comparator.Match getReqCompResult() {
        	return reqCompareRes;
        }

	    public Comparator.MatchType getReqCompareResType() {
		    return reqCompareRes.mt;
	    }

	    public List<Comparator.Diff> getReqDiffs() {
		    return reqCompareRes.diffs;
	    }


	    public Optional<String> getRecordedResponseBody() {
            return recordResp.map(event -> event.payload.getPayloadAsJsonString());
        }

        public Optional<String> getReplayResponseBody() {
            return replayResp.map(event -> event.payload.getPayloadAsJsonString());
        }

        public Optional<String> getReplayReq() {
            return replayReq.map(event -> event.payload.getPayloadAsJsonString());
        }


        public Optional<String> getRecordReq() {
	        return Optional.of(recordReq.payload.getPayloadAsJsonString());
        }
    }

    static public ReqRespMatchResult createReqRespMatchResult(ReqRespMatchWithEvent rm,
                                                              Comparator.MatchType reqmt, int size, String replayId) {
        return new ReqRespMatchResult(Optional.of(rm.recordReq.reqId),
            rm.replayReq.map(req -> req.reqId),
            reqmt,
            size,
            replayId,
            rm.recordReq.service,
            rm.recordReq.apiPath,
            Optional.of(rm.recordReq.getTraceId()),
            rm.replayReq.map(Event::getTraceId),
            Optional.ofNullable(rm.recordReq.spanId),
            Optional.ofNullable(rm.recordReq.parentSpanId),
            rm.replayReq.map(repEvent -> repEvent.spanId),
            rm.replayReq.map(repEvent -> repEvent.parentSpanId),
            rm.respCompareRes,
            rm.reqCompareRes);
    }


}
