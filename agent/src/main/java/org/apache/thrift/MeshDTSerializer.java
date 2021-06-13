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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.thrift.protocol.TBinaryProtocol.Factory;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TIOStreamTransport;

// https://github.com/apache/thrift/blob/master/lib/java/src/org/apache/thrift/TSerializer.java
public class MeshDTSerializer {
    private final ByteArrayOutputStream baos_;
    private final TIOStreamTransport transport_;
    private TProtocol protocol_;

    public MeshDTSerializer() {
        this(new Factory());
    }

    public MeshDTSerializer(TProtocolFactory protocolFactory) {
        this.baos_ = new ByteArrayOutputStream();
        this.transport_ = new TIOStreamTransport(this.baos_);
        this.protocol_ = protocolFactory.getProtocol(this.transport_);
    }

    // Changed input type from TBase to TSerializable - MESH-D
    public byte[] serialize(TSerializable base) throws TException {
        this.baos_.reset();
        base.write(this.protocol_);
        return this.baos_.toByteArray();
    }

    public String toString(TBase base, String charset) throws TException {
        try {
            return new String(this.serialize(base), charset);
        } catch (UnsupportedEncodingException var4) {
            throw new TException("JVM DOES NOT SUPPORT ENCODING: " + charset);
        }
    }

    public String toString(TBase base) throws TException {
        return new String(this.serialize(base));
    }

}
