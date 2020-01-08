package com.cube.dao;

import java.util.Optional;

import com.cube.core.Comparator;
import com.cube.core.Comparator.Match;
import com.cube.dao.Analysis.ReqRespMatchWithEvent;

public class ReqRespMatchResult {

	public ReqRespMatchResult(ReqRespMatchWithEvent rm,
		Comparator.MatchType reqmt, int size, String replayId) {
		this.recordReqId = Optional.of(rm.recordReq.reqId);
		this.replayReqId = rm.replayReq.map(req -> req.reqId);
		this.reqMatchRes = reqmt;
		this.numMatch = size;
		this.respCompareRes = rm.respMatch;
		this.reqCompareRes = rm.reqCompareRes;
		this.service = rm.recordReq.service;
		this.path = rm.recordReq.apiPath;
		this.replayId = replayId;
		this.recordTraceId = Optional.of(rm.recordReq.getTraceId());
		this.replayTraceId = rm.replayReq.map(Event::getTraceId);
	}

	public ReqRespMatchResult(Optional<String> recordReqId, Optional<String> replayReqId
		, Comparator.MatchType reqMatchRes, int numMatch, String replayId
		, String service, String path, Optional<String> recordTraceId
		, Optional<String> replayTraceId, Match responseMatch, Optional<Match> reqCompareRes) {
		this.recordReqId = recordReqId;
		this.replayReqId = replayReqId;
		this.reqMatchRes = reqMatchRes;
		this.numMatch = numMatch;
		this.replayId = replayId;
		this.service = service;
		this.path = path;
		this.recordTraceId = recordTraceId;
		this.replayTraceId = replayTraceId;
		this.respCompareRes = responseMatch;
		this.reqCompareRes = reqCompareRes;

	}


	public final Optional<String> recordReqId;
	public final Optional<String> replayReqId;
	public final Optional<String> recordTraceId;
	public final Optional<String> replayTraceId;

	public final Comparator.MatchType reqMatchRes;
	public final Comparator.Match respCompareRes;
	public final Optional<Comparator.Match> reqCompareRes;

	public final int numMatch;
	public final String service;
	public final String path;
	public final String replayId;
}

