package io.cube.agent.logging;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import io.md.utils.UtilException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;

import io.cube.agent.CommonConfig;
import io.cube.agent.CubeClient;
import io.md.constants.Constants;
import io.md.dao.Event;
import io.md.utils.CubeObjectMapperProvider;

public class ProxyEventBatchConsumer {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyEventBatchConsumer.class);

	private List<Event> temporaryBuffer = new ArrayList<>();

	private WriteLock writeLock = new ReentrantReadWriteLock().writeLock();

	private int maxQueueSize = CommonConfig.getInstance().disruptorConsumerMemoryBufferSize;

	private long lastTimeStamp = System.currentTimeMillis();

	private long maxWaitTimeMillis = 5000;

	//long totalTimeInMillis;
	//long count;


	public ProxyEventBatchConsumer() {
		new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(maxWaitTimeMillis);
				} catch (InterruptedException e) {
					LOGGER.error("Interrupt exception", e);
				}
				writeLock.lock();
				try {
					emptyBuffer();
				} catch (Exception e) {
					LOGGER.error("Error while emptying buffer", e);
				} finally {
					writeLock.unlock();
				}
			}
		}).start();
	}

	class MDEventEntity extends AbstractHttpEntity {

		private List<Event> mdEventList;
		private long contentLength;
		public MDEventEntity(List<Event> mdEvent) {
			this.mdEventList = mdEvent;
		}

		@Override
		public boolean isRepeatable() {
			return false;
		}

		@Override
		public long getContentLength() {
			return -1;
		}

		@Override
		public InputStream getContent() throws IOException, UnsupportedOperationException {
			return null;
		}

		@Override
		public void writeTo(OutputStream outputStream) throws IOException {
			mdEventList.forEach(UtilException.rethrowConsumer(mdEvent ->
			{
				outputStream.write("{ \"cubeEvent\" : ".getBytes());
				CubeObjectMapperProvider.getInstance().writeValue(outputStream, mdEvent);
				outputStream.write(" } ".getBytes());
				outputStream.write("\n".getBytes());
				if (LOGGER.isDebugEnabled()) {
					StringBuilder metaDataStringBuilder = new StringBuilder();
					if (mdEvent.metaData != null) {
						mdEvent.metaData.forEach((x, y) ->
							metaDataStringBuilder.append(x).append("->")
								.append(y).append(" "));
					}
					LOGGER.debug("SERIALIZED EVENT FOR BATCH REQUEST :: SERVICE :: " +
						mdEvent.service + " :: API PATH :: " + mdEvent.apiPath
						+ " :: REQ ID :: " + mdEvent.reqId + " :: TRACE ID :: "
						+ mdEvent.getTraceId() + " :: SPAN ID :: " + mdEvent.spanId
						+ " :: PARENT SPAN ID " + mdEvent.parentSpanId
						+ " :: EVENT TYPE :: " + mdEvent.eventType
						+ " :: EVENT META DATA " + metaDataStringBuilder.toString());
				}
			}));
			outputStream.flush();
		}

		@Override
		public boolean isStreaming() {
			return false;
		}
	}

	public void emptyBuffer() {
		long currentTime = System.currentTimeMillis();
		if (temporaryBuffer.size() >= maxQueueSize || (currentTime - lastTimeStamp >= 2000
			&& temporaryBuffer.size()
			> 0)) {
			List<Event> toWire = new ArrayList<>();
			toWire.addAll(temporaryBuffer);
			MDEventEntity eventEntity = new MDEventEntity(toWire);
			URI recordURI = URI.create(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.resolve("cs/").resolve("storeEventBatch");
			HttpPost recordReqbuilder = new HttpPost(recordURI);
			recordReqbuilder.setEntity(eventEntity);
			recordReqbuilder.setHeader("Content-Type", Constants.APPLICATION_X_NDJSON);
			Optional<String> response = CubeClient.getResponse(recordReqbuilder);
			LOGGER.info("Got Response for batch store event request " + response.orElse("NA"));
			temporaryBuffer.clear();
			lastTimeStamp = System.currentTimeMillis();
		}
	}

	public EventHandler<ValueEvent> getEventHandler() {
		return (event, sequence, endOfBatch)
			-> {
			//long startTime = System.currentTimeMillis();
			writeLock.lock();
			try {
				temporaryBuffer.add(event.getValue());
				emptyBuffer();
			} catch (Exception e) {
				LOGGER.error("Error while converting ");
			} finally {
				writeLock.unlock();
				/*totalTimeInMillis += System.currentTimeMillis() - startTime;
				count++;
				if (count % 100 == 0) {
					System.out.println("Time taken " + totalTimeInMillis*1.0/1000 + " millis for count : " +  count);
					count = 0;
					totalTimeInMillis = 0;
				}*/
			}

		};
	}

}
