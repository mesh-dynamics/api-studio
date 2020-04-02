package com.cube.drivers;

import java.util.Optional;

import com.cube.dao.Replay;
import com.cube.ws.Config;

public class ReplayDriverFactory {

	public static Optional<AbstractReplayDriver> initReplay(Replay replay, Config config) {
		AbstractReplayDriver driver;
		switch (replay.replayType) {
			case HTTP:
				driver = new HttpReplayDriver(replay, config);
				break;
			case THRIFT:
				driver = new ThriftReplayDriver(replay, config);
				break;
			default:
				return Optional.empty();
		}

		if (config.rrstore.saveReplay(driver.replay)) {
			return Optional.of(driver);
		} else {
			return Optional.empty();
		}

	}

}
