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
import com.lmax.disruptor.util.DaemonThreadFactory;

import io.cube.agent.logging.SingleEventPrintConsumer;
import io.cube.agent.logging.ValueEvent;
import io.md.dao.Event;
import io.opentracing.Scope;
import io.opentracing.Span;

public class ConsoleRecorder extends AbstractGsonSerializeRecorder {

	private static final Logger LOGGER = LogMgr.getLogger(ConsoleRecorder.class);

	private static ConsoleRecorder singleInstance;

	public Disruptor<ValueEvent> disruptor;
	public RingBuffer<ValueEvent> ringBuffer;

	AtomicLong droppedRequests = new AtomicLong();

	public static ConsoleRecorder getInstance() {
		if (singleInstance == null) {
			throw new AssertionError("Need to call init first!");
		}

		return singleInstance;
	}

	protected static synchronized ConsoleRecorder init() {
		if (singleInstance != null) {
			Disruptor<ValueEvent> disruptor = singleInstance.disruptor;
			new Thread(() -> doShutdown(disruptor)).start();
		}

		singleInstance = new ConsoleRecorder();
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
