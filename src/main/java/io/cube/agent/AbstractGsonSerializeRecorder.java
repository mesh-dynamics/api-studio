package io.cube.agent;


import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
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
	private Gson gson;

	public AbstractGsonSerializeRecorder(Gson gson) {
		// TODO pass this from above too
		this.jsonMapper = new ObjectMapper();
		jsonMapper.registerModule(new Jdk8Module());
		jsonMapper.registerModule(new JavaTimeModule());
		jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		this.gson = gson;
	}

	public abstract boolean record(FnReqResponse fnReqResponse);

	public abstract boolean record(Event event);

	@Override
	public boolean recordOld(FnKey fnKey, Optional<String> traceId,
		Optional<String> spanId,
		Optional<String> parentSpanId,
		Object responseOrException,
		RetStatus retStatus,
		Optional<String> exceptionType,
		Object... args) {
		try {
			String[] argVals =
				Arrays.stream(args).map(UtilException.rethrowFunction(gson::toJson))
					.toArray(String[]::new);
			Integer[] argsHash = Arrays.stream(argVals).map(String::hashCode)
				.toArray(Integer[]::new);
			//String respVal = jsonMapper.writeValueAsString(responseOrException);
			String respVal = gson.toJson(responseOrException);

			String traceIdString = traceId.orElse("N/A");
			var counter = new Object() {
				int x = 0;
			};
			Arrays.stream(argVals)
				.forEach(arg -> LOGGER.debug("func_name : ".concat(fnKey.fnName)
					.concat(" , trace_id : ").concat(traceIdString)
					.concat(" , arg_hash : ").concat(String.valueOf(argsHash[counter.x]))
					.concat(" , arg_val_".concat(String.valueOf(counter.x++))
						.concat(" : ").concat(arg))));

			LOGGER.info("return_value : ".concat(respVal));

			FnReqResponse fnrr = new FnReqResponse(fnKey.customerId, fnKey.app, fnKey.instanceId,
				fnKey.service,
				fnKey.fnSigatureHash, fnKey.fnName, traceId, spanId, parentSpanId,
				Optional.ofNullable(Instant.now()), argsHash,
				argVals, respVal, retStatus, exceptionType);
			return record(fnrr);
			//Optional<String> cubeResponse = cubeClient.storeFunctionReqResp(fnrr);
			//cubeResponse.ifPresent(responseStr -> System.out.println(responseStr));
			//return true;
		} catch (Exception e) {
			// encode can throw UnsupportedEncodingException
			String stackTraceError = UtilException
				.extractFirstStackTraceLocation(e.getStackTrace());
			LOGGER.error("func_name : ".concat(fnKey.fnName)
				.concat(" , trace_id : ").concat(traceId.orElse("NA")), e);
			return false;
		}
	}

	@Override
	public boolean record(FnKey fnKey, Optional<String> traceId,
		Optional<String> spanId,
		Optional<String> parentSpanId,
		Object responseOrException,
		RetStatus retStatus,
		Optional<String> exceptionType,
		Object... args) {
		try {
			MDTraceInfo mdTraceInfo = CommonUtils.mdTraceInfoFromContext();
			FnReqRespPayload fnReqRespPayload = new FnReqRespPayload(Optional.of(Instant.now()),
				args, responseOrException,retStatus , exceptionType);
			Optional<Event> event = CommonUtils.createEvent(fnKey, mdTraceInfo, Event.RunType.Record,
				Optional.of(Instant.now()), fnReqRespPayload);
			return event.map(ev -> record(ev)).orElseGet(() -> {
				LOGGER.error("func_name : ".concat(fnKey.fnName)
					.concat(" , trace_id : ").concat(traceId.orElse("NA"))
					.concat(" , operation : ".concat("Record Event")
						.concat(" , response : ").concat("Event is empty!")));
				return false;
			});
		} catch (Exception e) {
			// encode can throw UnsupportedEncodingException
			LOGGER.error("func_name : ".concat(fnKey.fnName)
				.concat(" , trace_id : ").concat(traceId.orElse("NA")), e);
			return false;
		}
	}
}
