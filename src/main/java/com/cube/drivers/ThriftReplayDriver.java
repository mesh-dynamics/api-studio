package com.cube.drivers;

import java.lang.reflect.Constructor;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import com.cube.dao.Event;
import com.cube.dao.Replay;
import com.cube.dao.ReqRespStore;
import com.cube.utils.Constants;
import com.cube.ws.Config;

public class ThriftReplayDriver extends AbstractReplayDriver {


	private static final Logger LOGGER = LogManager.getLogger(ThriftReplayDriver.class);
	private TDeserializer tDeserializer;

	public ThriftReplayDriver(Replay replay, Config config) {
		super(replay, config);
		this.tDeserializer = new TDeserializer();
	}


	static class ThriftRequest {

		public TBase args;
		public TBase result;
		String argsClassName;
		String resultClassName;
		String methodName;

		public ThriftRequest(byte[] binaryPayload, String apiPath, URLClassLoader classLoader,
			TDeserializer tDeserializer)
			throws Exception {
			String[] splits = apiPath.split("::");
			methodName = splits[0];
			argsClassName = splits[1];
			//TODO Assuming here that the result class name is the same except ending _result
			//instead of _args
			resultClassName = argsClassName.substring(0, argsClassName.indexOf("_args"))
				.concat("_result");
			Class<?> argsClazz = classLoader.loadClass(argsClassName);
			Constructor<?> constructor = argsClazz.getConstructor();
			args = (TBase) constructor.newInstance();
			tDeserializer.deserialize(args, binaryPayload);
			Class<?> resultClazz = classLoader.loadClass(resultClassName);
			constructor = resultClazz.getConstructor();
			result = (TBase) constructor.newInstance();
		}

	}


	protected void replay() {

		//List<Request> requests = getRequests();

		if (replay.status != Replay.ReplayStatus.Init) {
			return;
		}

		replay.status = Replay.ReplayStatus.Running;
		if (!rrstore.saveReplay(replay)) {
			return;
		}
		// This is a dummy lookup, just to get the Replay running status into Redis, so that deferred delete
		// can be applied when replay ends. This is needed for very small replays
		Optional<ReqRespStore.RecordOrReplay> recordOrReplay =
			rrstore.getCurrentRecordOrReplay(Optional.of(replay.customerId),
				Optional.of(replay.app), Optional.of(replay.instanceId));

		// start recording stats for the current replay
		//replayResultCache.startReplay(replay.customerId, replay.app, replay.instanceId, replay.replayId);

		// using seed generated from replayId so that same requests get picked in replay and analyze
		long seed = replay.replayId.hashCode();
		Random random = new Random(seed);

		// TODO get port from replay properties
		TTransport transport = new TSocket(replay.endpoint, 9090);
		try {
			transport.open();
		} catch (TTransportException e) {
		}
		TProtocol protocol = new TBinaryProtocol(transport);
		CubeThriftServiceClient thriftServiceClient = new CubeThriftServiceClient(protocol);

		Pair<Stream<List<Event>>, Long> batchedResult = replay.getRequestBatchesUsingEvents(
			BATCHSIZE, rrstore, jsonMapper);
		replay.reqcnt = batchedResult.getRight().intValue();

		batchedResult.getLeft().forEach(requests -> {

			// replay.reqcnt += requests.size();
			List<ThriftRequest> thriftRequests = new ArrayList<>();

			requests.forEach(eventReq -> {

				try {
					if (replay.sampleRate.map(sr -> random.nextDouble() > sr).orElse(false)) {
						return; // drop this request
					}
					// This will automatically send the span as from the original request
					ThriftRequest request = new ThriftRequest(eventReq.rawPayloadBinary
						, eventReq.apiPath, replay.generatedClassLoader, tDeserializer);

					thriftRequests.add(request);
					//TODO transform fields in the request before the replay.
				} catch (Exception e) {
					LOGGER.error(new ObjectMessage(Map.of(
						Constants.MESSAGE, "Skipping request. Exception in creating HTTP request "
					)), e);
				}
			});

			//TODO use async thrift request if applicable
			List<Integer> respcodes = sendReqSync(thriftRequests.stream(), thriftServiceClient);

			// count number of errors
			replay.reqfailed += respcodes.stream()
				.filter(s -> (s != 1)).count();
		});

		LOGGER.info(
			String.format("Replayed %d requests, got %d errors", replay.reqcnt, replay.reqfailed));

		replay.status =
			(replay.reqfailed == 0) ? Replay.ReplayStatus.Completed : Replay.ReplayStatus.Error;

		rrstore.saveReplay(replay);
	}


	private List<Integer> sendReqSync(Stream<ThriftRequest> thriftRequestStream
		, CubeThriftServiceClient thriftServiceClient) {
		return thriftRequestStream.map(request -> {
			try {
				replay.reqsent++;
				if (replay.reqsent % UPDBATCHSIZE == 0) {
					LOGGER.info(String.format("Replay %s completed %d requests", replay.replayId,
						replay.reqsent));
					rrstore.saveReplay(replay);
				}
				thriftServiceClient.sendBase(request.methodName, request.args);
				thriftServiceClient.receiveBase(request.result, request.methodName);
				return 1;
			} catch (Exception e) {
				//TODO form a proper error log
				LOGGER.error(new ObjectMessage(Map.of()), e);
				return 0;
			}
		}).collect(Collectors.toList());
	}
}


