package org.apache.thrift;

import java.util.Collections;
import java.util.Map;

import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TType;

// Using MeshDProcessFunction Everywhere instead of ProcessFunction - MESH-D
public class MeshDTBaseProcessor<I> implements TProcessor {
    private final I iface;
    private final Map<String, MeshDProcessFunction<I, ? extends TBase>> processMap;

    protected MeshDTBaseProcessor(I iface, Map<String, MeshDProcessFunction<I, ? extends TBase>> processFunctionMap) {
        this.iface = iface;
        this.processMap = processFunctionMap;
    }

    public Map<String, MeshDProcessFunction<I, ? extends TBase>> getProcessMapView() {
        return Collections.unmodifiableMap(processMap);
    }

    @Override
    public boolean process(TProtocol in, TProtocol out) throws TException {
        TMessage msg = in.readMessageBegin();
        MeshDProcessFunction fn = processMap.get(msg.name);
        if (fn == null) {
            TProtocolUtil.skip(in, TType.STRUCT);
            in.readMessageEnd();
            TApplicationException x = new TApplicationException(TApplicationException.UNKNOWN_METHOD, "Invalid method name: '"+msg.name+"'");
            out.writeMessageBegin(new TMessage(msg.name, TMessageType.EXCEPTION, msg.seqid));
            x.write(out);
            out.writeMessageEnd();
            out.getTransport().flush();
        } else {
            fn.process(msg.seqid, in, out, iface);
        }
        return true;
    }
}
