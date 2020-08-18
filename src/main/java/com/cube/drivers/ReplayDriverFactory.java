package com.cube.drivers;

import io.md.dao.Replay;
import io.md.services.DataStore;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ReplayDriverFactory {

	public static Optional<AbstractReplayDriver> initReplay(Replay replay, DataStore dataStore, ObjectMapper jsonMapper) {
		AbstractReplayDriver driver;
		switch (replay.replayType) {
			case HTTP:
				driver = new HttpReplayDriver(replay, dataStore, jsonMapper);
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
