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

package org.apache.thrift;

import java.util.Collections;
import java.util.Map;

import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TType;

// MESH-D - Using MeshDProcessFunction Everywhere instead of ProcessFunction
// https://github.com/apache/thrift/blob/master/lib/java/src/org/apache/thrift/TProcessor.java
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
    public void process(TProtocol in, TProtocol out) throws TException {
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
    }
}
