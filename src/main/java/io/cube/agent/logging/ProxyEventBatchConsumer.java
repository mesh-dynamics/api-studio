package io.cube.agent.logging;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.http.Consts;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
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

	public EventHandler<ValueEvent> getEventHandler() {
		return (event, sequence, endOfBatch  )
			-> {
			writeLock.lock();
			try {
				temporaryBuffer.add(event.getValue());
				long currentTime = System.currentTimeMillis();
				if (temporaryBuffer.size() >= maxQueueSize || (currentTime - lastTimeStamp >= 2000)) {
					StringBuilder builder = new StringBuilder();
					temporaryBuffer.forEach(
						bufferEvent -> {
							try {
								builder.append("{ \"cubeEvent\" : ");
								builder.append(
									CubeObjectMapperProvider.getInstance()
										.writeValueAsString(bufferEvent));
								builder.append(" } ");
								builder.append("\n");
								StringBuilder metaDataStringBuilder = new StringBuilder();
								if (bufferEvent.metaData != null) {
								bufferEvent.metaData.forEach((x, y) ->
									metaDataStringBuilder.append(x).append("->")
										.append(y).append(" "));}
								LOGGER.info("SERIALIZED EVENT FOR BATCH REQUEST :: SERVICE :: " +
									 bufferEvent.service + " :: API PATH :: " + bufferEvent.apiPath
								+ " :: REQ ID :: " + bufferEvent.reqId + " :: TRACE ID :: "
									+ bufferEvent.getTraceId() + " :: SPAN ID :: " + bufferEvent.spanId
								+ " :: PARENT SPAN ID " + bufferEvent.parentSpanId
									+ " :: EVENT TYPE :: "+  bufferEvent.eventType
									+ " :: EVENT META DATA "  + metaDataStringBuilder.toString());
							} catch (JsonProcessingException e) {
								LOGGER.error("Error while converting event to string", e);
							}
						}
					);
					String entity = builder.toString();
					URI recordURI = URI.create(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
						.resolve("cs/").resolve("storeEventBatch");
					HttpPost recordReqbuilder = new HttpPost(recordURI);
					StringEntity stringEntity = new StringEntity(entity, Consts.UTF_8);
					recordReqbuilder.setEntity(stringEntity);
					recordReqbuilder.setHeader("Content-Type", Constants.APPLICATION_X_NDJSON);
					Optional<String> response = CubeClient.getResponse(recordReqbuilder);
					LOGGER.info("Got Response for batch store event request " + response.orElse("NA"));
					temporaryBuffer.clear();
					lastTimeStamp = System.currentTimeMillis();
				}
			} catch (Exception e) {
				LOGGER.error("Error while converting ");
			} finally {
				writeLock.unlock();
			}

		};
	}

}
