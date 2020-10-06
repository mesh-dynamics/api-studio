package io.md.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import io.md.constants.Constants;
import io.md.core.ProtoDescriptor;
import io.md.core.WrapUnwrapContext;
import javax.ws.rs.core.MultivaluedMap;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GRPCPayload extends HTTPPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger(GRPCPayload.class);

    protected String path;
    protected String service;
    protected String method;
    @JsonIgnore
    private Optional<ProtoDescriptor> protoDescriptor;

    protected GRPCPayload(MultivaluedMap<String, String> hdrs, byte[] body, String path) {
        super(hdrs, body);
        this.path = path;
        parsePath();
    }

    protected GRPCPayload(JsonNode deserializedJsonTree) {
        super(deserializedJsonTree);
        try {
            this.path = this.dataObj.getValAsString("/".concat("path"));
        } catch (Exception e) {
            LOGGER.error("Error while initializing GRPC Payload" , e);
        }
        parsePath();
    }

    private void parsePath() {
        String[] pathSplits = this.path.split("/");
        this.service = pathSplits[0];
        if (service.contains(".")) service = service.substring(service.indexOf(".") + 1);
        this.method = pathSplits[1];
    }

    abstract boolean isRequest();

    private Optional<WrapUnwrapContext> getWrapUnwrapContext() {
        return protoDescriptor.map(descriptor ->
            new WrapUnwrapContext(descriptor, service, method, isRequest()));
    }

    @Override
    protected void unWrapBody() {
        // Currently unwrapAsJson does both decoding and unwrapping.
        // Will cleanup and separate the functions later
        if (payloadState == HTTPPayloadState.WrappedDecoded
            || payloadState == HTTPPayloadState.WrappedEncoded) {
            if (this.dataObj.unwrapAsJson("/".concat(HTTPRequestPayload.BODY),
                Constants.APPLICATION_GRPC,getWrapUnwrapContext())) {
                setPayloadState(HTTPPayloadState.UnwrappedDecoded);
            }
        }
    }

    @Override
    public void wrapBody() {

        if (payloadState == HTTPPayloadState.UnwrappedDecoded) {
            if (this.dataObj.wrapAsString("/".concat(HTTPRequestPayload.BODY),
                Constants.APPLICATION_GRPC, Optional.empty()))
                setPayloadState(HTTPPayloadState.WrappedDecoded);
        }
    }

    public void wrapBodyAndEncode() {
        if (payloadState == HTTPPayloadState.UnwrappedDecoded) {
            if (this.dataObj.wrapAsEncoded("/".concat(HTTPRequestPayload.BODY),
                    Constants.APPLICATION_GRPC, getWrapUnwrapContext()))
                setPayloadState(HTTPPayloadState.WrappedEncoded);
        }
    }

    @JsonIgnore
    public void setProtoDescriptor(Optional<ProtoDescriptor> protoDescriptor) {
        this.protoDescriptor = protoDescriptor;
        postParse();
    }
}
