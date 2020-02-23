package io.cube.agent;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.message.ObjectMessage;

import com.google.gson.Gson;

import io.md.constants.Constants;
import io.md.dao.DataObj;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;

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
			/*CommonConfig commonConfig = CommonConfig.getInstance();
			Optional<DataObj> payloadOptional = Optional.empty();

			final Span span = CommonUtils.startClientSpan("encryptEvent");
			try (Scope scope = CommonUtils.activateSpan(span)) {
				payloadOptional = Utils.encryptFields(commonConfig, event);
			} finally {
				span.finish();
			}
			// Using isPresent instead of ifPresentOrElse to avoid getting "Variable in Lambda should be final" for jsonSerialized;

			MDTraceInfo mdTraceInfo = new MDTraceInfo(event.getTraceId(), null, null);
			String jsonSerialized = "";
			final Span serializeSpan = CommonUtils.startClientSpan("jsonSerialize");
			try (Scope scope = CommonUtils.activateSpan(serializeSpan)) {
				jsonSerialized = payloadOptional.map(UtilException.rethrowFunction(payload -> {
					EventBuilder eventBuilder = new EventBuilder(event.customerId, event.app,
						event.service, event.instanceId,
						event.getCollection(), mdTraceInfo, event.runType,
						Optional.of(event.timestamp), event.reqId, event.apiPath, event.eventType);
					eventBuilder.setPayload(payload);
					eventBuilder.setRawPayloadString(payload.toString());
					return jsonMapper.writeValueAsString(eventBuilder.createEvent());
				}))
					.orElseGet(UtilException.rethrowSupplier(() -> {
						return jsonMapper.writeValueAsString(event);
					}));
			} finally {
				serializeSpan.finish();
			}*/

			// The prefix will be a part of the fluentd parse regex
			final Span logSpan = CommonUtils.startClientSpan("log4jLog");
			try (Scope scope = CommonUtils.activateSpan(logSpan)) {
				LOGGER.info(new ObjectMessage(Map.of("Cube Event" , event)));
			} finally {
				logSpan.finish();
			}
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
