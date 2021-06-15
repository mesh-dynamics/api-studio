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

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;

import io.md.utils.Constants;

// Just to access protected methods of abstract class
public class CubeThriftServiceClient extends TServiceClient {

	public static final Logger LOGGER = LogManager.getLogger(CubeThriftServiceClient.class);

	public CubeThriftServiceClient(TProtocol prot) {
		super(prot);
	}

	public CubeThriftServiceClient(TProtocol iprot, TProtocol oprot) {
		super(iprot, oprot);
	}

	/**
	 *
	 * @param methodName
	 * @param args
	 * @throws TException
	 */
	public void sendBase(String methodName, TBase<?, ?> args) throws TException {
		super.sendBase(methodName, args);
	}

	/**
	 *
	 * @param result
	 * @param methodName
	 * @throws TException
	 */
	public void receiveBase(TBase<?, ?> result, String methodName) throws TException {
		super.receiveBase(result, methodName);
	}

	public boolean flushTransport() {
		try {
			this.iprot_.getTransport().flush();
			this.oprot_.getTransport().flush();
			return true;
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE , "Flushing transport failed during replay")), e);
		}
		return false;
	}

}
