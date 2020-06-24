package io.cube.agent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.Gson;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;

import io.cube.agent.logging.ProxyEventBatchConsumer;
import io.cube.agent.logging.ValueEvent;
import io.md.dao.Event;
import io.opentracing.Scope;
import io.opentracing.Span;

public class ProxyBatchRecorder extends AbstractGsonSerializeRecorder {

	public Disruptor<ValueEvent> disruptor;
	public RingBuffer<ValueEvent> ringBuffer;

	AtomicLong droppedRequests = new AtomicLong();

	public ProxyBatchRecorder(Gson gson) {
		super(gson);


		ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;

		WaitStrategy waitStrategy = new BlockingWaitStrategy();
		ProxyEventBatchConsumer eventConsumer = new ProxyEventBatchConsumer();
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
		return false;
	}

	@Override
	public boolean record(ReqResp httpReqResp) {
		return false;
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
}
