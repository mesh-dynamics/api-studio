package io.cube.agent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.md.logger.LogMgr;
import org.slf4j.Logger;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import io.cube.agent.logging.ProxyEventBatchConsumer;
import io.cube.agent.logging.ValueEvent;
import io.md.dao.Event;
import io.opentracing.Scope;
import io.opentracing.Span;

public class ProxyBatchRecorder extends AbstractGsonSerializeRecorder {

	private static final Logger LOGGER = LogMgr.getLogger(ProxyBatchRecorder.class);
	private static ProxyBatchRecorder singleInstance;

	public Disruptor<ValueEvent> disruptor;
	public RingBuffer<ValueEvent> ringBuffer;

	AtomicLong droppedRequests = new AtomicLong();


	public static ProxyBatchRecorder getInstance() {
		if (singleInstance == null) {
			throw new AssertionError("Need to call init first!");
		}

		return singleInstance;
	}

	protected static synchronized ProxyBatchRecorder init() {
		if (singleInstance != null) {
			Disruptor<ValueEvent> disruptor = singleInstance.disruptor;
			new Thread(() -> doShutdown(disruptor)).start();
		}
		singleInstance = new ProxyBatchRecorder();
		return singleInstance;
	}

	private static void doShutdown(Disruptor<ValueEvent> disruptor) {
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

	private ProxyBatchRecorder() {
		super();

		ThreadFactory threadFactory = MDThreadFactory.INSTANCE;
		CubeClient cubeClient = new CubeClient(jsonMapper);

		WaitStrategy waitStrategy = new BlockingWaitStrategy();
		ProxyEventBatchConsumer eventConsumer = new ProxyEventBatchConsumer(cubeClient);
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
