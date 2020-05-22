package io.md.dao;

import java.util.Optional;

import io.md.core.Comparator;
import io.md.core.Comparator.Match;

public class ReqRespMatchResult {

    public ReqRespMatchResult(Optional<String> recordReqId, Optional<String> replayReqId
		, Comparator.MatchType reqMatchRes, int numMatch, String replayId
		, String service, String path, Optional<String> recordTraceId
		, Optional<String> replayTraceId, Optional<String> recordedSpanId, Optional<String> recordedParentSpanId
        , Optional<String> replayedSpanId, Optional<String> replayedParentSpanId
        , Match responseMatch, Match reqCompareRes) {
		this.recordReqId = recordReqId;
		this.replayReqId = replayReqId;
		this.reqMatchRes = reqMatchRes;
		this.numMatch = numMatch;
		this.replayId = replayId;
		this.service = service;
		this.path = path;
		this.recordTraceId = recordTraceId;
		this.replayTraceId = replayTraceId;
		this.recordedSpanId = recordedSpanId;
		this.recordedParentSpanId = recordedParentSpanId;
		this.replayedSpanId = replayedSpanId;
		this.replayedParentSpanId = replayedParentSpanId;
		this.respCompareRes = responseMatch;
		this.reqCompareRes = reqCompareRes;

	}


	public final Optional<String> recordReqId;
	public final Optional<String> replayReqId;
	public final Optional<String> recordTraceId;
	public final Optional<String> replayTraceId;
	public final Optional<String> recordedSpanId;
	public final Optional<String> recordedParentSpanId;
    public final Optional<String> replayedSpanId;
    public final Optional<String> replayedParentSpanId;

	public final Comparator.MatchType reqMatchRes;
	public final Match respCompareRes;
	public final Match reqCompareRes;

	public final int numMatch;
	public final String service;
	public final String path;
	public final String replayId;
}

