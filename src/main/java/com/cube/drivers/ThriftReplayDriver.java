package com.cube.drivers;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import io.md.dao.Event;

import com.cube.core.Utils;
import io.md.dao.Replay;
import io.md.dao.ResponsePayload;
import io.md.dao.ThriftResponsePayload;

import com.cube.utils.Constants;
import com.cube.ws.Config;

//import com.cube.dao.Event;

public class ThriftReplayDriver extends AbstractReplayDriver {


	private static final Logger LOGGER = LogManager.getLogger(ThriftReplayDriver.class);


	ThriftReplayDriver(Replay replay, Config config) {
		super(replay, config);
	}

	@Override
	public IReplayClient initClient(Replay replay) throws  Exception {
		return new ThriftReplayClient(replay.endpoint);
	}

	static class ThriftReplayClient implements IReplayClient {

		private TDeserializer tDeserializer;
		private CubeThriftServiceClient thriftServiceClient;
		private TTransport transport;

		ThriftReplayClient(String replayEndpoint) throws Exception {
			// TODO extract port from replay endpoint as well

			this.tDeserializer = new TDeserializer();
			transport = new TSocket(replayEndpoint, 9090);
			transport.open();
			TProtocol protocol = new TBinaryProtocol(transport);
			thriftServiceClient = new CubeThriftServiceClient(protocol);

		}

		@Override
		public ResponsePayload send(Event requestEvent, Replay replay) {
			try {
				ThriftRequest thriftRequest = build(requestEvent, replay);
				thriftServiceClient.sendBase(thriftRequest.methodName, thriftRequest.args);
				thriftServiceClient.receiveBase(thriftRequest.result, thriftRequest.methodName);
				thriftServiceClient.flushTransport();
				return new ThriftResponsePayload(1);
			} catch (Exception e) {
				return new ThriftResponsePayload(0);
			}
		}

		@Override
		public CompletableFuture<ResponsePayload> sendAsync(Event requestEvent, Replay replay) {
			return null;
		}

		public ThriftRequest build(Event reqEvent, Replay replay)
			throws IOException {
			try {
				Map<String, Object> params = Utils.extractThriftParams(reqEvent.apiPath);
				if (params.get(Constants.THRIFT_CLASS_NAME) == null) {
					throw new IOException("Could not extract class name from API path");
				}
				String methodName = (String) params.get(Constants.THRIFT_METHOD_NAME);
				URLClassLoader classLoader = replay.generatedClassLoader.orElseThrow(()
					-> new IOException("Unable to retrieve class loader from replay"));
				String argsClassName = (String) params.get(Constants.THRIFT_CLASS_NAME);
				//TODO Assuming here that the result class name is the same except ending _result
				//instead of _args
				String resultClassName = argsClassName.substring(0, argsClassName.indexOf("_args"))
					.concat("_result");
				Class<?> argsClazz = classLoader.loadClass(argsClassName);
				Constructor<?> constructor = argsClazz.getConstructor();
				TBase args = (TBase) constructor.newInstance();
				tDeserializer.deserialize(args, reqEvent.payload.rawPayloadAsByteArray());
				Class<?> resultClazz = classLoader.loadClass(resultClassName);
				constructor = resultClazz.getConstructor();
				TBase result = (TBase) constructor.newInstance();
				return new ThriftRequest(args, result, methodName);
			} catch (Exception e) {
				throw new IOException(e);
			}
		}

		@Override
		public boolean isSuccessStatusCode(String responseCode) {
			Optional<Integer> intResponse = Utils.strToInt(responseCode);
			return intResponse.map(intCode -> {
				return intCode==1 ? true : false;
			}).orElse(false);
		}

		@Override
		public String getErrorStatusCode() {
			return String.valueOf(0);
		}

		@Override
		public boolean tearDown() {
			this.transport.close();
			return true;
		}
	}

	static class ThriftRequest implements IReplayRequest {

		public TBase args;
		public TBase result;
		String methodName;

		public ThriftRequest(TBase args, TBase result, String methodName) {
			this.args = args;
			this.result = result;
			this.methodName = methodName;
		}

	}
}


