package com.cube.dao;

import java.util.Optional;

import javax.ws.rs.core.MultivaluedMap;

import com.cube.core.Comparator;
import com.cube.core.Utils;
import com.cube.utils.Constants;

public class AnalysisMatchResultQuery {

	public final String replayId;
	public final Optional<String> service;
	public final Optional<String> apiPath;
	public final Optional<Comparator.MatchType> reqMatchType;
	public final Optional<Comparator.MatchType> reqCompResType;
	public final Optional<Comparator.MatchType> respCompResType;
	public final Optional<Integer> start;
	public final Optional<Integer> numMatches;
	public final Optional<String> diffResolution;
	public final Optional<String> diffType;
	public final Optional<String> diffJsonPath;
	public final Optional<String> traceId;
	public final Optional<String> recordReqId;
	public final Optional<String> replayReqId;
	//public final Optional<String> replayReqTraceId;
	/*public final Optional<String> recParentSpanId;
	public final Optional<String> replayParentSpanId*/

	public AnalysisMatchResultQuery(String replayId, MultivaluedMap<String, String> queryParams) {
		this.replayId =replayId;
		this.service = Optional
			.ofNullable(queryParams.getFirst(Constants.SERVICE_FIELD));
		this.apiPath = Optional
			.ofNullable(queryParams.getFirst(Constants.PATH_FIELD)); // the path to drill down on
		this.start = Optional.ofNullable(queryParams.getFirst(Constants.START_FIELD))
			.flatMap(Utils::strToInt); // for paging
		this.numMatches =
			Optional.ofNullable(queryParams.getFirst(Constants.NUM_RESULTS_FIELD))
				.flatMap(Utils::strToInt); // for paging
		this.reqMatchType = Optional
			.ofNullable(queryParams.getFirst(Constants.REQ_MATCH_TYPE))
			.flatMap(v -> Utils.valueOf(Comparator.MatchType.class, v));
		this.respCompResType = Optional
			.ofNullable(queryParams.getFirst(Constants.RESP_MATCH_TYPE))
			.flatMap(v -> Utils.valueOf(Comparator.MatchType.class, v));
		this.reqCompResType = Optional
			.ofNullable(queryParams.getFirst(Constants.REQ_COMP_RES_TYPE))
			.flatMap(v -> Utils.valueOf(Comparator.MatchType.class, v));
		this.diffResolution = Optional.ofNullable(queryParams
			.getFirst(Constants.DIFF_RESOLUTION_FIELD));
		this.diffJsonPath = Optional.ofNullable(queryParams
			.getFirst(Constants.DIFF_PATH_FIELD));
		this.diffType = Optional.ofNullable(queryParams
			.getFirst(Constants.DIFF_TYPE_FIELD));
		this.traceId = Optional.ofNullable(queryParams.getFirst(Constants.TRACE_ID_FIELD));
		this.recordReqId = Optional.ofNullable(queryParams.getFirst(Constants.RECORD_REQ_ID_FIELD));
		this.replayReqId = Optional.ofNullable(queryParams.getFirst(Constants.REPLAY_REQ_ID_FIELD));

	}

}
