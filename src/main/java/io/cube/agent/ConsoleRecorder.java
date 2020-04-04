package io.cube.agent;

import com.google.gson.Gson;

import io.cube.agent.logging.ValueEvent;
import io.md.dao.Event;
import io.opentracing.Scope;
import io.opentracing.Span;

public class ConsoleRecorder extends AbstractGsonSerializeRecorder {


	public ConsoleRecorder(Gson gson) {
		super(gson);
	}

	@Override
	public boolean record(FnReqResponse fnReqResponse) {
		try {
			// TODO might wanna explore java fluent logger
			// https://github.com/fluent/fluent-logger-java
			String jsonSerialized = jsonMapper.writeValueAsString(fnReqResponse);
			// The prefix will be a part of the fluentd parse regex
			LOGGER.info("[Cube FnReqResp Event]" + jsonSerialized);
			return true;
		} catch (Exception e) {
			LOGGER.error("Unable to serialize Function Req Response Object", e);
			return false;
		}
	}

	@Override
	public boolean record(Event event) {
		final Span span = Utils.createPerformanceSpan("log4jLog");
		try (Scope scope = Utils.activatePerformanceSpan(span)) {
			long sequenceId = CommonConfig.ringBuffer.next();
			ValueEvent valueEvent = CommonConfig.ringBuffer.get(sequenceId);
			valueEvent.setValue(event);
			CommonConfig.ringBuffer.publish(sequenceId);
			return true;
		} catch (Exception e) {
			LOGGER.error("Unable to serialize Event Object", e);
			return false;
		} finally {
			span.finish();
		}
	}

	@Override
	public boolean record(ReqResp httpReqResp) {
		try {
			String jsonSerialized = jsonMapper.writeValueAsString(httpReqResp);
			// The prefix will be a part of the fluentd parse regex
			LOGGER.info("[Cube ReqResp]" + jsonSerialized);
			return true;
		} catch (Exception e) {
			LOGGER.error("Unable to serialize ReqResp Object", e);
			return false;
		}
	}
}
