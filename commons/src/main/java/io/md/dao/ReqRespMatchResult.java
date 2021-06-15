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

package io.md.dao;

import io.md.core.Comparator.MatchType;
import java.util.Optional;

import io.md.core.Comparator;
import io.md.core.Comparator.Match;

public class ReqRespMatchResult {

	// for Jackson serialization
	private ReqRespMatchResult() {
		this.recordReqId = Optional.empty();
		this.replayReqId = Optional.empty();
		this.reqMatchRes = MatchType.Default;
		this.numMatch = 0;
		this.replayId = "";
		this.service = "";
		this.path = "";
		this.recordTraceId = Optional.empty();
		this.replayTraceId = Optional.empty();
		this.recordedSpanId = Optional.empty();
		this.recordedParentSpanId = Optional.empty();
		this.replayedSpanId = Optional.empty();
		this.replayedParentSpanId = Optional.empty();
		this.respCompareRes = Match.DEFAULT;
		this.reqCompareRes = Match.DEFAULT;
	}

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

