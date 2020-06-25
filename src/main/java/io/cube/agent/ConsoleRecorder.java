package io.cube.agent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutException;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleRecorder.class);

	private static ConsoleRecorder singleInstance;

	public static Disruptor<ValueEvent> disruptor;
	public RingBuffer<ValueEvent> ringBuffer;

	AtomicLong droppedRequests = new AtomicLong();

	public static ConsoleRecorder getInstance() {
		if (singleInstance == null) {
			throw new AssertionError("Need to call init first!");
		}

		return singleInstance;
	}

	protected static synchronized ConsoleRecorder init() {
		//TODO: stop and clear the earlier buffer.
		singleInstance = new ConsoleRecorder();
		new Thread(ConsoleRecorder::doShutdown).start();
		return singleInstance;
	}

	private static void doShutdown() {
		if (disruptor != null) {
			long timeoutms =  60000;
			try {
				disruptor.shutdown(timeoutms, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				LOGGER.info("Timed out bringing down disruptor after " + timeoutms + "ms; forcing halt ");
				disruptor.halt();
				disruptor.shutdown();
			}
		}
	}

	private ConsoleRecorder() {
		super();

		ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;

		WaitStrategy waitStrategy = new BlockingWaitStrategy();
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
