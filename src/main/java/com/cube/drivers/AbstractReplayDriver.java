package com.cube.drivers;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TDeserializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.dao.Replay;
import com.cube.dao.ReqRespStore;
import com.cube.ws.Config;

public abstract class AbstractReplayDriver {

	private static Logger LOGGER = LogManager.getLogger(AbstractReplayDriver.class);
	protected final Replay replay;
	public final ReqRespStore rrstore;
	protected final Config config;
	protected ObjectMapper jsonMapper;


	static int UPDBATCHSIZE = 10; // replay metadata will be updated after each such batch
	static int BATCHSIZE = 40; // this controls the number of requests in a batch that
	// could be sent in async fashion

	public AbstractReplayDriver(Replay replay, Config config) {
		super();
		this.replay = replay;
		this.rrstore = config.rrstore;
		this.jsonMapper = config.jsonMapper;
		this.config = config;
	}

	public boolean start() {

		if (replay.status != Replay.ReplayStatus.Init) {
			String message = String
				.format("Replay with id %s is already running or completed", replay.replayId);
			LOGGER.error(message);
			return false;
		}
		LOGGER.info(String.format("Starting replay with id %s", replay.replayId));
		CompletableFuture.runAsync(this::replay).handle((ret, e) -> {
			if (e != null) {
				LOGGER.error("Exception in replaying requests", e);
			}
			return ret;
		});
		return true;
	}

	protected abstract void replay();

	public Replay getReplay() {
		return replay;
	}


	public static Optional<Replay> getStatus(String replayId, ReqRespStore rrstore) {
		return rrstore.getReplay(replayId);
	}

	// TODO write this function properly to return appropriate ReplayDriver
	//  not being used anywhere currently (will have to store replayType as a part of Replay
	public static Optional<AbstractReplayDriver> getReplayDriver(String replayId, Config config) {
		return getStatus(replayId, config.rrstore).map(r -> new HttpReplayDriver(r, config));
	}


}
