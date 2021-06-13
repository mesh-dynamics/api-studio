/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cube.dao;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedMap;

import io.md.core.Comparator;
import io.md.core.Utils;
import io.md.utils.Constants;

public class AnalysisMatchResultQuery {

	public final List<String> replayId;
	public final Optional<String> service;
	public final List<String> apiPaths;
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

	public AnalysisMatchResultQuery(List<String> replayId) {
		this.replayId = replayId;
		service = Optional.empty();
		apiPaths = Collections.emptyList();
		reqMatchType = Optional.empty();
		reqCompResType = Optional.empty();
		respCompResType = Optional.empty();
		start = Optional.empty();
		numMatches = Optional.empty();
		diffJsonPath = Optional.empty();
		diffResolution = Optional.empty();
		diffType = Optional.empty();
		traceId = Optional.empty();
		recordReqId = Optional.empty();
		replayReqId = Optional.empty();

	}

	public AnalysisMatchResultQuery(String replayId) {
	    this(Collections.singletonList(replayId));
    }


	public AnalysisMatchResultQuery(String replayId, MultivaluedMap<String, String> queryParams) {
		this.replayId = Collections.singletonList(replayId);
		this.service = Optional
			.ofNullable(queryParams.getFirst(Constants.SERVICE_FIELD));
        this.apiPaths = Optional.ofNullable(queryParams.get(Constants.PATH_FIELD)) // the path to drill down on
            .orElse(Collections.emptyList());
		this.start = Optional.ofNullable(queryParams.getFirst(Constants.START_FIELD))
			.flatMap(Utils::strToInt); // for paging
		this.numMatches =
			Optional.ofNullable(queryParams.getFirst(Constants.NUM_RESULTS_FIELD))
				.flatMap(Utils::strToInt).or(() -> Optional.of(20)); // for paging
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
