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

package io.md.drivers;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.cache.ProtoDescriptorCache;
import io.md.dao.Replay;
import io.md.services.DataStore;

public class ReplayDriverFactory {

	public static Optional<AbstractReplayDriver> initReplay(Replay replay, DataStore dataStore, ObjectMapper jsonMapper, Optional<ProtoDescriptorCache> protoDescriptorCacheOptional) {
		AbstractReplayDriver driver;
		switch (replay.replayType) {
			case HTTP:
				driver = new HttpReplayDriver(replay, dataStore, jsonMapper);
				break;
			case GRPC:
				driver = new GrpcReplayDriver(replay, dataStore, jsonMapper, protoDescriptorCacheOptional);
				break;
//			case THRIFT:
//				driver = new ThriftReplayDriver(replay, config);
//				break;
			default:
				return Optional.empty();
		}

		if (dataStore.saveReplay(driver.replay)) {
			return Optional.of(driver);
		} else {
			return Optional.empty();
		}

	}

}
