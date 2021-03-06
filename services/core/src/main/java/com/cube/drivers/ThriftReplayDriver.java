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

import io.md.core.Utils;
import io.md.dao.Replay;
import io.md.dao.ResponsePayload;
import io.md.dao.ThriftResponsePayload;
import io.md.drivers.AbstractReplayDriver;
import io.md.utils.Constants;

import com.cube.ws.Config;

//import com.cube.dao.Event;

public class ThriftReplayDriver extends AbstractReplayDriver {


	private static final Logger LOGGER = LogManager.getLogger(ThriftReplayDriver.class);


	ThriftReplayDriver(Replay replay, Config config) {
		super(replay, config.rrstore);
	}

	@Override
	public IReplayClient initClient(Replay replay) throws  Exception {
		return new ThriftReplayClient(replay.endpoint);
	}

	@Override
	protected void modifyResponse(Event event) {
		return;
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


