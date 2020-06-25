package io.cube.agent;

import java.io.FileNotFoundException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.MDTraceInfo;
import io.md.dao.Payload;
import io.md.dao.Recording.RecordingType;
import io.opentracing.Scope;
import io.opentracing.Span;

public class EncryptConsoleRecorder {

	private static final Logger LOGGER = LoggerFactory.getLogger(EncryptConsoleRecorder.class);

	public boolean record(Event event) {
		try {
			CommonConfig commonConfig = CommonConfig.getInstance();
			ConsoleRecorder recorder = ConsoleRecorder.getInstance();
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
						event.getCollection(), mdTraceInfo, event.getRunType(),
						Optional.of(event.timestamp), event.reqId, event.apiPath, event.eventType, event.recordingType)
						.setPayload(payload);
					return eventBuilder.createEvent();
				})).orElse(event);
			} finally {
				eventCreate.finish();
			}
			return recorder.record(encryptedEvent);
		} catch (Exception e) {
			LOGGER.error("Unable to serialize Event Object", e);
			return false;
		}
	}
}
