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

package io.cube.agent;


import java.time.Instant;
import java.util.Optional;

import io.md.logger.LogMgr;
import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.md.dao.Event;
import io.md.dao.FnReqRespPayload;
import io.md.dao.FnReqRespPayload.RetStatus;
import io.md.dao.MDTraceInfo;
import io.md.dao.Recording.RecordingType;
import io.md.utils.CommonUtils;
import io.md.utils.FnKey;

public abstract class AbstractGsonSerializeRecorder implements Recorder {

	protected final Logger LOGGER = LogMgr.getLogger(this.getClass());

	protected ObjectMapper jsonMapper;

	public AbstractGsonSerializeRecorder() {
		// TODO pass this from above too
		this.jsonMapper = new ObjectMapper();
		jsonMapper.registerModule(new Jdk8Module());
		jsonMapper.registerModule(new JavaTimeModule());
		jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
	}

	public abstract boolean record(FnReqResponse fnReqResponse);

	public abstract boolean record(Event event);

	@Override
	public boolean  record(FnKey fnKey, Object responseOrException, RetStatus retStatus,
		Optional<String> exceptionType, String runId,Object... args) {

		MDTraceInfo mdTraceInfo;
		if (CommonUtils.getCurrentTraceId().isPresent()) {
			//load the created context
			mdTraceInfo = CommonUtils.mdTraceInfoFromContext();

		} else {
			//No span context. Initialization scenario.
			mdTraceInfo = CommonUtils.getDefaultTraceInfo();
		}

		try {
			FnReqRespPayload fnReqRespPayload = new FnReqRespPayload(Optional.of(Instant.now()),
				args, responseOrException, retStatus, exceptionType);
			Optional<Event> event = CommonUtils
				.createEvent(fnKey, mdTraceInfo, Event.RunType.Record,
					Optional.of(Instant.now()), fnReqRespPayload, RecordingType.Golden, runId);
			return event.map(ev -> record(ev)).orElseGet(() -> {
				LOGGER.error("func_name : ".concat(fnKey.fnName)
					.concat(" , trace_id : ").concat(mdTraceInfo.traceId)
					.concat(" , operation : ".concat("Record Event")
						.concat(" , response : ").concat("Event is empty!")));
				return false;
			});
		} catch (Exception e) {
			// encode can throw UnsupportedEncodingException
			LOGGER.error("func_name : ".concat(fnKey.fnName)
				.concat(" , trace_id : ").concat(mdTraceInfo.traceId), e);
			return false;
		}
	}
}
