package org.apache.thrift;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.transport.TTransportException;

import com.google.gson.GsonBuilder;

import io.cube.agent.CommonConfig;
import io.cube.agent.FluentDLogRecorder;
import io.cube.agent.ThriftMocker;
import io.md.constants.Constants;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.EventType;
import io.md.dao.Event.RunType;
import io.md.utils.CommonUtils;

// MESH-D Mostly overriding the process function in
// https://github.com/apache/thrift/blob/master/lib/java/src/org/apache/thrift/ProcessFunction.java
public abstract class MeshDProcessFunction<I, T extends TBase> {

	private final String methodName;

	private static final Logger LOGGER = LogManager.getLogger(MeshDProcessFunction.class.getName());


	public MeshDProcessFunction(String methodName) {
		this.methodName = methodName;
		try {
			fluentDLogRecorder = new FluentDLogRecorder((new GsonBuilder()).create());
			serializer = new MeshDTSerializer();
			thriftMocker = new ThriftMocker();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private FluentDLogRecorder fluentDLogRecorder;
	private String reqId;
	private MeshDTSerializer serializer;
	private ThriftMocker thriftMocker;


	private String constructApiPath(String methodName, TSerializable argsOrResult) {
		return methodName + "::" + argsOrResult.getClass().getName();
	}

	// MESH-D All the logic for record and mock goes inside this function
	// it was declared final in the original class and hence could not be overridden
	public final void process(int seqid, TProtocol iprot, TProtocol oprot, I iface)
		throws TException {
		T args = getEmptyArgsInstance();
		try {
			args.read(iprot);
			// TODO add prefix serviceName + "-" once common config is being injected
			reqId = CommonUtils.getCurrentTraceId().orElse("NA") + "-"
				+ UUID.randomUUID().toString();
			try {
				if (CommonConfig.isIntentToRecord()) {
					EventBuilder eventBuilder = new EventBuilder(CommonUtils.cubeMetaInfoFromEnv(),
						CommonUtils.cubeTraceInfoFromContext(), RunType.Record,
						constructApiPath(methodName, args),
						EventType.ThriftRequest, Optional.of(Instant.now()), reqId,
						Constants.DEFAULT_COLLECTION)
						.setRawPayloadBinary(serializer.serialize(args));
					fluentDLogRecorder.record(eventBuilder.createEvent());
				}
			} catch (Exception e) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
					"Error while recording event")), e);
			}
		} catch (TProtocolException e) {
			iprot.readMessageEnd();
			TApplicationException x = new TApplicationException(
				TApplicationException.PROTOCOL_ERROR, e.getMessage());
			oprot.writeMessageBegin(new TMessage(getMethodName(), TMessageType.EXCEPTION, seqid));
			x.write(oprot);
			oprot.writeMessageEnd();
			oprot.getTransport().flush();
			return;
		}
		iprot.readMessageEnd();
		TSerializable result = null;
		byte msgType = TMessageType.REPLY;

		try {
			if (CommonConfig.isIntentToMock()) {
				EventBuilder eventBuilder = new EventBuilder(CommonUtils.cubeMetaInfoFromEnv(),
					CommonUtils.cubeTraceInfoFromContext(), RunType.Replay,
					constructApiPath(methodName, args),
					EventType.ThriftRequest, Optional.of(Instant.now()), reqId,
					Constants.DEFAULT_COLLECTION)
					.setRawPayloadBinary(serializer.serialize(args));
				result = thriftMocker.mockThriftRequest(eventBuilder.createEvent());
				LOGGER.info(new ObjectMessage(
					Map.of(Constants.MESSAGE,
						"Successfully retrieved result from mock service")));
			} else {
				result = getResult(iface, args);
			}
		} catch (TTransportException ex) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Transport error while processing", "methodName", getMethodName())), ex);
			throw ex;
		} catch (TApplicationException ex) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE
				, "Internal application error while processing"
				, "methodName", getMethodName())), ex);
			result = ex;
			msgType = TMessageType.EXCEPTION;
		} catch (Exception ex) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Transport error while processing", "methodName", getMethodName())), ex);
			if (rethrowUnhandledExceptions()) {
				throw new RuntimeException(ex.getMessage(), ex);
			}
			if (!isOneway()) {
				result = new TApplicationException(TApplicationException.INTERNAL_ERROR,
					"Internal error processing " + getMethodName());
				msgType = TMessageType.EXCEPTION;
			}
		}

		if (CommonConfig.isIntentToRecord()) {
			try {
				EventBuilder eventBuilder = new EventBuilder(CommonUtils.cubeMetaInfoFromEnv(),
					CommonUtils.cubeTraceInfoFromContext(), RunType.Record,
					constructApiPath(methodName, result),
					EventType.ThriftResponse, Optional.of(Instant.now()), reqId,
					Constants.DEFAULT_COLLECTION)
					.setRawPayloadBinary(serializer.serialize(result));
				fluentDLogRecorder.record(eventBuilder.createEvent());
			} catch (Exception e) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
					"Error while recording event")), e);
			}
		}
		if (!isOneway()) {
			oprot.writeMessageBegin(new TMessage(getMethodName(), msgType, seqid));
			result.write(oprot);
			oprot.writeMessageEnd();
			oprot.getTransport().flush();
		}
	}

	private void handleException(int seqid, TProtocol oprot) throws TException {
		if (!isOneway()) {
			TApplicationException x = new TApplicationException(
				TApplicationException.INTERNAL_ERROR,
				"Internal error processing " + getMethodName());
			oprot.writeMessageBegin(new TMessage(getMethodName(), TMessageType.EXCEPTION, seqid));
			x.write(oprot);
			oprot.writeMessageEnd();
			oprot.getTransport().flush();
		}
	}

	protected boolean rethrowUnhandledExceptions() {
		return false;
	}

	protected abstract boolean isOneway();

	public abstract TBase getResult(I iface, T args) throws TException;

	public abstract T getEmptyArgsInstance();

	public String getMethodName() {
		return methodName;
	}
}