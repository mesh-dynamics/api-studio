package com.cube.queue;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.lmax.disruptor.EventHandler;

import io.md.dao.Event;
import io.md.dao.MDStorable;

import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStore.ReqResp;
import com.cube.utils.Constants;

public class StoreConsumer {
	private static final Logger LOGGER = LogManager.getLogger(StoreConsumer.class);

	private ReqRespStore reqRespStore;

	public StoreConsumer(ReqRespStore reqRespStore) {
		this.reqRespStore = reqRespStore;
		LOGGER.debug("Store Consumer Constructor Called");
	}

	public EventHandler<DisruptorValue> getEventHandler() {
		return (event, sequence, endOfBatch)
			-> {
			try {
				MDStorable toStore = event.getValue();
				if (toStore instanceof Event)
					StoreUtils.processEvent((Event) toStore, reqRespStore);
				else if (toStore instanceof RREvent) {
					RREvent rrEvent = (RREvent) toStore;
					StoreUtils.storeSingleReqResp(rrEvent.rr, rrEvent.path, rrEvent.queryParams,
						reqRespStore);
				}
			} catch (Exception e) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
					"Error while storing event/rr in solr")), e);
			}
		};
	}
}
