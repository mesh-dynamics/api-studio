/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cube.queue;

import java.util.Optional;
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

import io.md.cache.ProtoDescriptorCache;
import io.md.dao.MDStorable;

import com.cube.dao.ReqRespStore;

public class DisruptorEventQueue {

	public Disruptor<DisruptorValue> disruptor;
	public RingBuffer<DisruptorValue> ringBuffer;

	AtomicLong droppedRequests = new AtomicLong();

	protected final Logger LOGGER = LogManager.getLogger(this.getClass());

	public DisruptorEventQueue(ReqRespStore reqRespStore, int queueSize, Optional<ProtoDescriptorCache> protoDescriptorCacheOptional) {
		ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;

		WaitStrategy waitStrategy = new BlockingWaitStrategy();
		StoreConsumer eventConsumer = new StoreConsumer(reqRespStore, protoDescriptorCacheOptional);
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
