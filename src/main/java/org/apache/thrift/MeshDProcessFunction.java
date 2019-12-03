package org.apache.thrift;

import java.util.UUID;

import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

import io.cube.agent.CommonUtils;
import io.cube.agent.Event.EventType;
import io.cube.agent.Event.RunType;
import io.cube.agent.EventBuilder;
import io.cube.agent.FluentDLogRecorder;
import io.cube.agent.ThriftMocker;

public abstract class MeshDProcessFunction<I, T extends TBase> {
    private final String methodName;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessFunction.class.getName());


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

    // MESH-D All the logic for record and mock goes inside this function
    // it was declared final in the original class and hence could not be overridden
    public final void process(int seqid, TProtocol iprot, TProtocol oprot, I iface)
        throws TException {
        T args = getEmptyArgsInstance();
        try {
            args.read(iprot);
            reqId = UUID.randomUUID().toString();
            try {
                if (CommonUtils.isIntentToRecord()) {
                    EventBuilder eventBuilder = new EventBuilder(CommonUtils.cubeMetaInfoFromEnv(),
                        CommonUtils.cubeTraceInfoFromContext(), RunType.Record,
                        methodName + "::" + args.getClass().getName(),
                        EventType.ThriftRequest).withRawPayloadBinary(serializer.serialize(args))
                        .withReqId(reqId);
                    fluentDLogRecorder.record(eventBuilder.build());
                }
            } catch (Exception e) {
                LOGGER.error("Error while recording event" , e);
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
            if (CommonUtils.isIntentToMock()) {
                EventBuilder eventBuilder = new EventBuilder(CommonUtils.cubeMetaInfoFromEnv(),
                    CommonUtils.cubeTraceInfoFromContext(), RunType.Replay,
                    methodName + "::" + args.getClass().getName(),
                    EventType.ThriftRequest).withRawPayloadBinary(serializer.serialize(args))
                    .withReqId(reqId);
                result = thriftMocker.mockThriftRequest(eventBuilder.build());
                LOGGER.info("Successfully retrieved result from mock service");
            } else {
                result = getResult(iface, args);
            }
        } catch (TTransportException ex) {
            LOGGER.error("Transport error while processing " + getMethodName(), ex);
            throw ex;
        } catch (TApplicationException ex) {
            LOGGER.error("Internal application error processing " + getMethodName(), ex);
            result = ex;
            msgType = TMessageType.EXCEPTION;
        } catch (Exception ex) {
            LOGGER.error("Internal error processing " + getMethodName(), ex);
            if (rethrowUnhandledExceptions()) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
            if (!isOneway()) {
                result = new TApplicationException(TApplicationException.INTERNAL_ERROR,
                    "Internal error processing " + getMethodName());
                msgType = TMessageType.EXCEPTION;
            }
        }

        if (CommonUtils.isIntentToRecord()) {
            try {
                EventBuilder eventBuilder = new EventBuilder(CommonUtils.cubeMetaInfoFromEnv(),
                    CommonUtils.cubeTraceInfoFromContext(), RunType.Record,
                    methodName + "::" + result.getClass().getName(),
                    EventType.ThriftResponse).withRawPayloadBinary(serializer.serialize(result))
                    .withReqId(reqId);
                fluentDLogRecorder.record(eventBuilder.build());
            } catch (Exception e) {
                LOGGER.error("Error while recording event", e);
            }
        }
        if(!isOneway()) {
            oprot.writeMessageBegin(new TMessage(getMethodName(), msgType, seqid));
            result.write(oprot);
            oprot.writeMessageEnd();
            oprot.getTransport().flush();
        }
    }

    private void handleException(int seqid, TProtocol oprot) throws TException {
        if (!isOneway()) {
            TApplicationException x = new TApplicationException(TApplicationException.INTERNAL_ERROR,
                "Internal error processing " + getMethodName());
            oprot.writeMessageBegin(new TMessage(getMethodName(), TMessageType.EXCEPTION, seqid));
            x.write(oprot);
            oprot.writeMessageEnd();
            oprot.getTransport().flush();
        }
    }

    protected boolean rethrowUnhandledExceptions(){
        return false;
    }

    protected abstract boolean isOneway();

    public abstract TBase getResult(I iface, T args) throws TException;

    public abstract T getEmptyArgsInstance();

    public String getMethodName() {
        return methodName;
    }
}