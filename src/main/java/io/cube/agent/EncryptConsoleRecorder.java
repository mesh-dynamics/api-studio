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
import io.md.dao.Payload;
import io.opentracing.Scope;
import io.opentracing.Span;

public class EncryptConsoleRecorder extends ConsoleRecorder {

	public EncryptConsoleRecorder(Gson gson) {
		super(gson);
	}

	public boolean record(Event event) {
		try {
			CommonConfig commonConfig = CommonConfig.getInstance();
			Optional<Payload> payloadOptional;
			// TODO make encryptFields return AbstractMDPayload instead of DataObj
			final Span span = Utils.createPerformanceSpan("encryptPayload");
			try (Scope scope = Utils.activatePerformanceSpan(span)) {
				payloadOptional = Utils.encryptFields(commonConfig, event);
			} finally {
				span.finish();
			}

			MDTraceInfo mdTraceInfo = new MDTraceInfo(event.getTraceId(), null, null);
			Event  encryptedEvent  = null;
			final Span eventCreate = Utils.createPerformanceSpan("postEncryptEventCreate");
			try (Scope scope = Utils.activatePerformanceSpan(eventCreate)) {
				encryptedEvent = payloadOptional.map(UtilException.rethrowFunction(payload -> {
					EventBuilder eventBuilder = new EventBuilder(event.customerId, event.app,
						event.service, event.instanceId,
						event.getCollection(), mdTraceInfo, event.runType,
						Optional.of(event.timestamp), event.reqId, event.apiPath, event.eventType)
						.setPayload(payload);
					return eventBuilder.createEvent();
				})).orElse(event);
			} finally {
				eventCreate.finish();
			}
			return super.record(encryptedEvent);
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(
				Map.of(io.md.constants.Constants.MESSAGE, "Unable to serialize Event Object", Constants.REASON,
					e.getMessage())));
			return false;
		}
	}
}
