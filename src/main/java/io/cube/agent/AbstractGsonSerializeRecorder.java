package io.cube.agent;


import static io.md.utils.CommonUtils.createPayload;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.md.dao.Event;
import io.md.dao.Event.RunType;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.md.utils.FnKey;

public abstract class AbstractGsonSerializeRecorder implements Recorder {

	protected final Logger LOGGER = LogManager.getLogger(this.getClass());

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
		FnReqResponse.RetStatus retStatus,
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
				.forEach(arg -> LOGGER.info(new ObjectMessage(Map.of("func_name", fnKey.fnName
					, "trace_id", traceIdString, "arg_hash", argsHash[counter.x],
					"arg_val_" + counter.x++, arg))));

			LOGGER.info(new ObjectMessage(Map.of("return_value", respVal)));

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
			LOGGER.error(new ObjectMessage(
				Map.of("func_name", fnKey.fnName, "trace_id", traceId.orElse("NA"))), e);
			return false;
		}
	}

	@Override
	public boolean record(FnKey fnKey, Optional<String> traceId,
		Optional<String> spanId,
		Optional<String> parentSpanId,
		Object responseOrException,
		FnReqResponse.RetStatus retStatus,
		Optional<String> exceptionType,
		Object... args) {
		try {
			JsonObject payload = createPayload(responseOrException, gson, args);
			MDTraceInfo mdTraceInfo = new MDTraceInfo(traceId.orElse(null),
				spanId.orElse(null), parentSpanId.orElse(null));

			Optional<Event> event = CommonUtils.creacreateEvent(fnKey, mdTraceInfo, RunType.Record,
				Optional.of(Instant.now()), payload);
			return event.map(ev -> record(ev)).orElseGet(() -> {
				LOGGER.error(new ObjectMessage(Map.of("func_name", fnKey.fnName, "trace_id",
					traceId.orElse("NA"), "operation", "Record Event", "response",
					"Event is empty!")));
				return false;
			});

		} catch (Exception e) {
			// encode can throw UnsupportedEncodingException
			String stackTraceError = UtilException
				.extractFirstStackTraceLocation(e.getStackTrace());
			LOGGER.error(new ObjectMessage(
				Map.of("func_name", fnKey.fnName, "trace_id", traceId.orElse("NA"))), e);
			return false;
		}
	}
}
