package com.cube.drivers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.core.Utils;
import com.cube.dao.Event;
import com.cube.dao.Replay;
import com.cube.dao.ReqRespStore;
import com.cube.utils.Constants;
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

	AbstractReplayDriver(Replay replay, Config config) {
		super();
		this.replay = replay;
		this.rrstore = config.rrstore;
		this.jsonMapper = config.jsonMapper;
		this.config = config;
	}

	public abstract IReplayClient initClient(Replay replay) throws Exception;

	public boolean start() {

		if (replay.status != Replay.ReplayStatus.Init) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Replay already running or completed", Constants.REPLAY_ID_FIELD
				, replay.replayId)));
			return false;
		}
		try {
			this.client = initClient(replay);
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Unable to initialize replay client")), e);
		}
		LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Starting Replay",
			Constants.REPLAY_ID_FIELD , replay.replayId)));
		CompletableFuture.runAsync(this::replay).handle((ret, e) -> {
			if (e != null) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
					"Exception in replaying requests")), e);
			}
			return ret;
		});
		return true;
	}

	interface IReplayClient {

		int send(IReplayRequest request) throws IOException, InterruptedException;

		CompletableFuture<Integer> sendAsync(IReplayRequest request);

		IReplayRequest build(Replay replay, Event reqEvent, Config config)
			throws IOException;

		int getSuccessStatusCode();

		int getErrorStatusCode();
	}

	// this is just a marker interface
	interface IReplayRequest {

	}

	private IReplayClient client;

	protected void replay() {

		//List<Request> requests = getRequests();

		if (replay.status != Replay.ReplayStatus.Init) {
			return;
		}
		replay.status = Replay.ReplayStatus.Running;
		if (!rrstore.saveReplay(replay)) {
			return;
		}
		// This is a dummy lookup, just to get the Replay running status into Redis, so that
		// deferred delete  can be applied when replay ends. This is needed for very small replays
		Optional<ReqRespStore.RecordOrReplay> recordOrReplay =
			rrstore.getCurrentRecordOrReplay(Optional.of(replay.customerId),
				Optional.of(replay.app), Optional.of(replay.instanceId));

		// using seed generated from replayId so that same requests get picked in replay and analyze
		long seed = replay.replayId.hashCode();
		Random random = new Random(seed);

		// TODO: add support for matrix params

		Pair<Stream<List<Event>>, Long> batchedResult = replay
			.getRequestBatchesUsingEvents(BATCHSIZE, rrstore,
				jsonMapper);
		replay.reqcnt = batchedResult.getRight().intValue();
		// NOTE: converting long to int, should be ok, since we
		// never replay so many requests

		batchedResult.getLeft().forEach(requests -> {

			// replay.reqcnt += requests.size();

			List<IReplayRequest> reqs = new ArrayList<>();
			requests.forEach(eventReq -> {

				try {
					/*
                     TODO: currently sampling samples across all paths with same rate.
                      If we want to ensure that we have some minimum requests from each path
                      (particularly the rare ones), we need to add more logic
                    */
					if (replay.sampleRate.map(sr -> random.nextDouble() > sr).orElse(false)) {
						return; // drop this request
					}

					reqs.add(client.build(replay, eventReq, config));

				} catch (Exception e) {
					LOGGER.error(new ObjectMessage(Map.of(
						Constants.MESSAGE, "Skipping request. Exception in Creating Replay Request"
						, Constants.REQ_ID_FIELD, eventReq.reqId != null ? eventReq.reqId : "NA"
					)), e);
				}
			});

			List<Integer> respcodes = replay.async ? sendReqAsync(reqs.stream(), client)
				: sendReqSync(reqs.stream(), client);

			// count number of errors
			replay.reqfailed += respcodes.stream()
				.filter(s -> (s != client.getSuccessStatusCode())).count();
		});

		LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Replay Completed"
			, Constants.REPLAY_ID_FIELD, replay.replayId,
			"Total Requests", replay.reqcnt, "Total Errors", replay.reqfailed)));

		replay.status =
			(replay.reqfailed == 0) ? Replay.ReplayStatus.Completed : Replay.ReplayStatus.Error;

		rrstore.saveReplay(replay);
	}

	private void logUpdate() {
		if (replay.reqsent % UPDBATCHSIZE == 0) {
			LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Replay Update",
				Constants.REPLAY_ID_FIELD, replay.replayId, "Requests Sent", replay.reqsent)));
			rrstore.saveReplay(replay);
		}
	}


	private List<Integer> sendReqAsync(Stream<IReplayRequest> replayRequests,
		IReplayClient client) {
		// exceptions are converted to status code indicating error
		List<CompletableFuture<Integer>> respcodes = replayRequests.map(request -> {
			replay.reqsent++;
			logUpdate();
			return client.sendAsync(request);
		}).collect(Collectors.toList());
		CompletableFuture<List<Integer>> rcodes = Utils.sequence(respcodes);

		try {
			return rcodes.get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(
				new ObjectMessage(Map.of(Constants.MESSAGE, "Exception in replaying requests")), e);
			return Collections
				.nCopies(respcodes.size(), client.getErrorStatusCode());
		}
	}

	private List<Integer> sendReqSync(Stream<IReplayRequest> requests,
		IReplayClient client) {

		return requests.map(request -> {
			try {
				replay.reqsent++;
				logUpdate();
				int ret = client.send(request);
				// for debugging - can remove later
				if (ret != client.getSuccessStatusCode()) {
					LOGGER.error(new ObjectMessage(
						Map.of(Constants.MESSAGE, "Got Error Status while Replaying Request",
							Constants.REQUEST, request.toString(), "Return Status", ret)));
				}
				return ret;
			} catch (IOException | InterruptedException e) {
				LOGGER.error(
					new ObjectMessage(Map.of(Constants.MESSAGE, "Exception in replaying requests")),
					e);
				return client.getErrorStatusCode();
			}
		}).collect(Collectors.toList());
	}

	public Replay getReplay() {
		return replay;
	}


	public static Optional<Replay> getStatus(String replayId, ReqRespStore rrstore) {
		return rrstore.getReplay(replayId);
	}


}
