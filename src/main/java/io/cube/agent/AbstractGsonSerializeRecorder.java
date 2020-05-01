package io.cube.agent;


import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;

import io.md.dao.Event;
import io.md.dao.FnReqRespPayload;
import io.md.dao.FnReqRespPayload.RetStatus;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.md.utils.FnKey;

public abstract class AbstractGsonSerializeRecorder implements Recorder {

	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	protected ObjectMapper jsonMapper;

	public AbstractGsonSerializeRecorder(Gson gson) {
		// TODO pass this from above too
		this.jsonMapper = new ObjectMapper();
		jsonMapper.registerModule(new Jdk8Module());
		jsonMapper.registerModule(new JavaTimeModule());
		jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
	}

	public abstract boolean record(FnReqResponse fnReqResponse);

	public abstract boolean record(Event event);

	@Override
	public boolean record(FnKey fnKey, Object responseOrException, RetStatus retStatus,
		Optional<String> exceptionType, Object... args) {

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
					Optional.of(Instant.now()), fnReqRespPayload);
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
