package org.apache.thrift;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.thrift.protocol.TBinaryProtocol.Factory;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TIOStreamTransport;

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
