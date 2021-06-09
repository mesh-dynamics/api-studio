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
