package com.cube.drivers;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransportException;

import com.cube.utils.Constants;

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
