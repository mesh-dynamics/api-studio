package com.cube.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;

import io.md.dao.Event;
import io.md.dao.MDStorable;

import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStore.ReqResp;

public class StoreConsumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(StoreConsumer.class);

	private ReqRespStore reqRespStore;

	public StoreConsumer(ReqRespStore reqRespStore) {
		this.reqRespStore = reqRespStore;
		LOGGER.debug("Store Consumer Constructor Called");
	}

	public EventHandler<DisruptorValue> getEventHandler() {
		return (event, sequence, endOfBatch)
			-> {
			MDStorable toStore = event.getValue();
			if (toStore instanceof Event)
				StoreUtils.processEvent((Event)toStore,reqRespStore);
			else if (toStore instanceof RREvent) {
				RREvent rrEvent = (RREvent) toStore;
				StoreUtils.storeSingleReqResp(rrEvent.rr, rrEvent.path,rrEvent.queryParams, reqRespStore);
			}
		};
	}
}
