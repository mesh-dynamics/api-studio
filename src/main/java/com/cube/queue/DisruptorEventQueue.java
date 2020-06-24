package com.cube.queue;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;

import io.md.dao.MDStorable;

import com.cube.dao.ReqRespStore;

public class DisruptorEventQueue {

	public Disruptor<DisruptorValue> disruptor;
	public RingBuffer<DisruptorValue> ringBuffer;

	AtomicLong droppedRequests = new AtomicLong();

	protected final Logger LOGGER = LogManager.getLogger(this.getClass());

	public DisruptorEventQueue(ReqRespStore reqRespStore, int queueSize) {
		ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;

		WaitStrategy waitStrategy = new BlockingWaitStrategy();
		StoreConsumer eventConsumer = new StoreConsumer(reqRespStore);
		disruptor
			= new Disruptor<>(
			DisruptorValue.getEventFactory(),
			queueSize,
			threadFactory,
			ProducerType.MULTI,
			waitStrategy);
		disruptor.handleEventsWith(eventConsumer.getEventHandler());
		ringBuffer = disruptor.start();
	}

	public boolean enqueue(MDStorable event) {
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
		try {
			DisruptorValue valueEvent = ringBuffer.get(sequenceId);
			valueEvent.setValue(event);
			return true;
		} catch (Exception e) {
			LOGGER.error("Unable to enqueue Event Object", e);
			return false;
		} finally {
			ringBuffer.publish(sequenceId);
		}
	}

}
