package io.cube.agent;

import java.util.Map;

import org.apache.logging.log4j.message.ObjectMessage;

import com.google.gson.Gson;

public class FluentDLogRecorder extends AbstractGsonSerializeRecorder {

	public FluentDLogRecorder(Gson gson) {
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
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE, "Unable to serialize Function Req Response Object",
					Constants.REASON, e.getMessage())));
			return false;
		}
	}

	@Override
	public boolean record(Event event) {
		try {
			// TODO might wanna explore java fluent logger
			// https://github.com/fluent/fluent-logger-java
			String jsonSerialized = jsonMapper.writeValueAsString(event);
			// The prefix will be a part of the fluentd parse regex
			LOGGER.info("[Cube Event]" + jsonSerialized);
			return true;
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE, "Unable to serialize Event Object", Constants.REASON,
					e.getMessage())));
			return false;
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
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE, "Unable to serialize ReqResp Object", Constants.REASON,
					e.getMessage())));
			return false;
		}
	}
}
