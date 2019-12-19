package com.cube.drivers;

import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;

// Just to access protected methods of abstract class
public class CubeThriftServiceClient extends TServiceClient {

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

}
