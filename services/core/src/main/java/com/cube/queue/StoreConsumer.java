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

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.lmax.disruptor.EventHandler;

import io.md.cache.ProtoDescriptorCache;
import io.md.dao.Event;
import io.md.dao.MDStorable;
import io.md.utils.Constants;

import com.cube.dao.ReqRespStore;

public class StoreConsumer {
	private static final Logger LOGGER = LogManager.getLogger(StoreConsumer.class);

	private ReqRespStore reqRespStore;
	private Optional<ProtoDescriptorCache> protoDescriptorCacheOptional;

	public StoreConsumer(ReqRespStore reqRespStore, Optional<ProtoDescriptorCache> protoDescriptorCacheOptional) {
		this.reqRespStore = reqRespStore;
		this.protoDescriptorCacheOptional = protoDescriptorCacheOptional;
		LOGGER.debug("Store Consumer Constructor Called");
	}

	public EventHandler<DisruptorValue> getEventHandler() {
		return (event, sequence, endOfBatch)
			-> {
			try {
				MDStorable toStore = event.getValue();
				if (toStore instanceof Event)
					StoreUtils.processEvent((Event) toStore, reqRespStore, protoDescriptorCacheOptional);
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
