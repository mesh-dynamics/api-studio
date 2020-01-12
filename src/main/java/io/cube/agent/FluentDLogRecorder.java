package io.cube.agent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.logging.log4j.message.ObjectMessage;

import com.google.gson.Gson;

import io.md.constants.Constants;
import io.md.dao.DataObj;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.EventType;
import io.md.dao.Event.RunType;
import io.md.utils.CommonUtils;

public class FluentDLogRecorder extends AbstractGsonSerializeRecorder {

	@Inject
	CommonConfig commonConfig;

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
			Optional<DataObj> payloadOptional = Utils.encryptFields(commonConfig, event);
			String jsonSerialized;

			// Using isPresent instead of ifPresentOrElse to avoid getting "Variable in Lambda should be final" for jsonSerialized;
			if(payloadOptional.isPresent()) {
				DataObj payload = payloadOptional.get();
				EventBuilder eventBuilder = new EventBuilder(event.customerId, event.app, event.service, event.instanceId,
				event.getCollection(), event.getTraceId(), event.runType, event.timestamp, event.reqId, event.apiPath, event.eventType);
				eventBuilder.setPayload(payload);
				eventBuilder.setRawPayloadString(payload.toString());
				jsonSerialized = jsonMapper.writeValueAsString(eventBuilder.createEvent());
			} else {
				jsonSerialized = jsonMapper.writeValueAsString(event);
			}

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
