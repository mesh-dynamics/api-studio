package io.cube.agent;

import java.io.FileNotFoundException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.Gson;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;

import io.cube.agent.logging.SingleEventPrintConsumer;
import io.cube.agent.logging.ValueEvent;
import io.md.dao.Event;
import io.opentracing.Scope;
import io.opentracing.Span;

public class ConsoleRecorder extends AbstractGsonSerializeRecorder {

	public Disruptor<ValueEvent> disruptor;
	public RingBuffer<ValueEvent> ringBuffer;

	AtomicLong droppedRequests = new AtomicLong();

	public ConsoleRecorder(Gson gson) throws FileNotFoundException {
		super(gson);

		ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;

		WaitStrategy waitStrategy = new BusySpinWaitStrategy();
		SingleEventPrintConsumer eventConsumer = new SingleEventPrintConsumer();
		disruptor
			= new Disruptor<>(
			ValueEvent.EVENT_FACTORY,
			CommonConfig.getInstance().ringBufferSize,
			threadFactory,
			ProducerType.MULTI,
			waitStrategy);
		disruptor.handleEventsWith(eventConsumer.getEventHandler());
		ringBuffer = disruptor.start();

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
		long sequenceId = 0;
		try {
			sequenceId = ringBuffer.tryNext();
		} catch (InsufficientCapacityException e) {
			if (droppedRequests.incrementAndGet()%1000 == 0) {
				LOGGER.info("Number of requests dropped so far "
					+ droppedRequests.get());
			}
			return false;
		}
		try (Scope scope = Utils.activatePerformanceSpan(span)) {
			ValueEvent valueEvent = ringBuffer.get(sequenceId);
			valueEvent.setValue(event);
			return true;
		} catch (Exception e) {
			LOGGER.error("Unable to serialize Event Object", e);
			return false;
		} finally {
			span.finish();
			ringBuffer.publish(sequenceId);
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
