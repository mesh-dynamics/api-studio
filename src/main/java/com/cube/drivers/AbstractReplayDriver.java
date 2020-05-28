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

import io.md.constants.ReplayStatus;
import io.md.dao.Event;
import io.md.dao.RecordOrReplay;
import io.md.dao.Replay;
import io.md.services.DataStore;

import com.cube.core.Utils;
import com.cube.dao.CubeMetaInfo;
import com.cube.dao.ReplayUpdate;
import com.cube.dao.ReqRespStore;
import com.cube.injection.DynamicInjectionConfig;
import com.cube.utils.Constants;
import com.cube.ws.Config;

public abstract class AbstractReplayDriver {

	private static Logger LOGGER = LogManager.getLogger(AbstractReplayDriver.class);
	protected final Replay replay;
	public final ReqRespStore rrstore;
	protected final Config config;
	protected ObjectMapper jsonMapper;
	Optional<DynamicInjectionConfig> dynamicInjectionConfig;


	static int UPDBATCHSIZE = 10; // replay metadata will be updated after each such batch
	static int BATCHSIZE = 40; // this controls the number of requests in a batch that
	// could be sent in async fashion

	AbstractReplayDriver(Replay replay, Config config) {
		super();
		this.replay = replay;
		this.rrstore = config.rrstore;
		this.jsonMapper = config.jsonMapper;
		this.config = config;
		replay.dynamicInjectionConfigVersion.map(DIConfVersion -> {
			return rrstore.getDynamicInjectionConfig(
				new CubeMetaInfo(replay.customerId, replay.app, replay.instanceId), DIConfVersion);
		}).orElse(Optional.empty());

	}

	public abstract IReplayClient initClient(Replay replay) throws Exception;

	public boolean start(boolean analyze) {

		if (replay.status != ReplayStatus.Init) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Replay already running or completed", Constants.REPLAY_ID_FIELD
				, replay.replayId)));
			return false;
		}
		try {
			this.client = initClient(replay);
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Unable to initialize replay client", Constants.REPLAY_ID_FIELD, replay.replayId)), e);
		}
		LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Starting Replay",
			Constants.REPLAY_ID_FIELD , replay.replayId)));
		CompletableFuture.runAsync(() -> replay(analyze)).handle((ret, e) -> {
			if (e != null) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
					"Exception in replaying requests", Constants.REPLAY_ID_FIELD, replay.replayId)), e);
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

		boolean isSuccessStatusCode(int statusCode);

		int getErrorStatusCode();

		boolean tearDown();
	}

	// this is just a marker interface
	interface IReplayRequest {

	}

	private IReplayClient client;

	protected void replay(boolean analyze) {

		//List<Request> requests = getRequests();

		if (replay.status != ReplayStatus.Init) {
			return;
		}
		replay.status = ReplayStatus.Running;
		if (!rrstore.saveReplay(replay)) {
			return;
		}
		// This is a dummy lookup, just to get the Replay running status into Redis, so that
		// deferred delete  can be applied when replay ends. This is needed for very small replays
		Optional<RecordOrReplay> recordOrReplay =
			rrstore.getCurrentRecordOrReplay(Optional.of(replay.customerId),
				Optional.of(replay.app), Optional.of(replay.instanceId));

		// using seed generated from replayId so that same requests get picked in replay and analyze
		long seed = replay.replayId.hashCode();
		Random random = new Random(seed);

		// TODO: add support for matrix params

		Pair<Stream<List<Event>>, Long> batchedResult = ReplayUpdate
			.getRequestBatchesUsingEvents(BATCHSIZE, rrstore, replay);
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
					LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE, "Enqueuing request"
						+ "for reply", Constants.REPLAY_ID_FIELD , replay.replayId
						, Constants.REQ_ID_FIELD, Optional.ofNullable(eventReq.reqId)
							.orElse(Constants.NOT_PRESENT), Constants.TRACE_ID_FIELD, eventReq.getTraceId())));
					reqs.add(client.build(replay, eventReq, config));

				} catch (Exception e) {
					LOGGER.error(new ObjectMessage(Map.of(
						Constants.MESSAGE, "Skipping request. Exception in Creating Replay Request"
						, Constants.REQ_ID_FIELD, Optional.ofNullable(eventReq.reqId).orElse(Constants.NOT_PRESENT)
						, Constants.REPLAY_ID_FIELD, replay.replayId
					)), e);
				}
			});

			List<Integer> respcodes = replay.async ? sendReqAsync(reqs.stream())
				: sendReqSync(reqs.stream());

			// count number of errors
			replay.reqfailed += respcodes.stream()
				.filter(s -> (!client.isSuccessStatusCode(s))).count();
		});

		LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Replay Completed"
			, Constants.REPLAY_ID_FIELD, replay.replayId,
			"totalRequests", replay.reqcnt, "errorRequests", replay.reqfailed)));

		replay.status =
			(replay.reqfailed == 0) ? ReplayStatus.Completed : ReplayStatus.Error;

		rrstore.saveReplay(replay);
		if (analyze) {
			analyze();
		}
		this.client.tearDown();
	}

	private void logUpdate() {
		if (replay.reqsent % UPDBATCHSIZE == 0) {
			LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Replay Update",
				Constants.REPLAY_ID_FIELD, replay.replayId, "sentRequests", replay.reqsent
				, "totalRequests", replay.reqcnt)));
			rrstore.saveReplay(replay);
		}
	}


	private List<Integer> sendReqAsync(Stream<IReplayRequest> replayRequests) {
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
				new ObjectMessage(Map.of(Constants.MESSAGE, "Exception in replaying requests",
					Constants.REPLAY_ID_FIELD, replay.replayId)), e);
			return Collections
				.nCopies(respcodes.size(), client.getErrorStatusCode());
		}
	}

	private List<Integer> sendReqSync(Stream<IReplayRequest> requests) {

		return requests.map(request -> {
			try {
				replay.reqsent++;
				logUpdate();
				int ret = client.send(request);
				// for debugging - can remove later
				if (!client.isSuccessStatusCode(ret)) {
					LOGGER.error(new ObjectMessage(
						Map.of(Constants.MESSAGE, "Got Error Status while Replaying Request",
							Constants.REQUEST, request.toString(), "Return Status", ret
						,Constants.REPLAY_ID_FIELD, replay.replayId)));
				}
				return ret;
			} catch (IOException | InterruptedException e) {
				LOGGER.error(
					new ObjectMessage(Map.of(Constants.MESSAGE, "Exception in replaying requests"
					,Constants.REPLAY_ID_FIELD , replay.replayId)), e);
				return client.getErrorStatusCode();
			}
		}).collect(Collectors.toList());
	}

	public void analyze() {
		ReplayStatus status = ReplayStatus.Running;
		while( status == ReplayStatus.Running) {
			try {
				Thread.sleep(5000);
				Optional<Replay> currentRunningReplay = rrstore.getCurrentRecordOrReplay(Optional.of(replay.customerId),
						Optional.of(replay.app), Optional.of(replay.instanceId))
						.flatMap(runningRecordOrReplay -> runningRecordOrReplay.replay);
				status = currentRunningReplay.filter(runningReplay -> runningReplay.
						replayId.equals(replay.replayId)).map(r -> r.status).orElse(replay.status);
			} catch (InterruptedException e) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
						"Exception while sleeping  the thread", Constants.REPLAY_ID_FIELD
						, replay.replayId)));
			}
		}
		try {
			Analyzer.analyze(replay.replayId, "", config);
		} catch (DataStore.TemplateNotFoundException e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
					"Unable to analyze replay since template does not exist :", Constants.REPLAY_ID_FIELD,
					replay.replayId)), e);
		}
	}

	public Replay getReplay() {
		return replay;
	}


	public static Optional<Replay> getStatus(String replayId, ReqRespStore rrstore) {
		return rrstore.getReplay(replayId);
	}


}
