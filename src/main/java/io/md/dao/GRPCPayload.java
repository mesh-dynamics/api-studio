package io.md.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import io.md.constants.Constants;
import io.md.core.WrapUnwrapContext;
import io.md.logger.LogMgr;
import io.md.utils.Utils;

import javax.ws.rs.core.MultivaluedMap;

import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;

public abstract class GRPCPayload extends HTTPPayload {

    private static final Logger LOGGER = LogMgr.getLogger(GRPCPayload.class);

    protected String path;
    protected String protoService;
    protected String methodName;
    @JsonIgnore
    private Optional<ProtoDescriptorDAO> protoDescriptor;

    protected GRPCPayload(MultivaluedMap<String, String> hdrs, byte[] body, String path) {
        super(hdrs, body);
        this.path = path;
        parsePath();
    }

    protected GRPCPayload(JsonNode deserializedJsonTree) {
        super(deserializedJsonTree);
        try {
            this.path = this.dataObj.getValAsString("/".concat("path"));
            this.payloadState = Utils.valueOf(HTTPPayload.HTTPPayloadState.class, this.dataObj.getValAsString(HTTPPayload.PAYLOADSTATEPATH)).orElse(HTTPPayloadState.UnwrappedDecoded);
        } catch (Exception e) {
            LOGGER.error("Error while initializing GRPC Payload" , e);
        }
        parsePath();
    }

    private void parsePath() {
        String[] pathSplits = this.path.split("/");
        this.protoService = pathSplits[0];
        if (protoService.contains(".")) protoService = protoService
            .substring(protoService.indexOf(".") + 1);
        this.methodName = pathSplits[1];
    }

    abstract boolean isRequest();

    @Override
    protected Optional<WrapUnwrapContext> getWrapUnwrapContext() {
        return protoDescriptor.map(descriptor ->
            new WrapUnwrapContext(descriptor, protoService, methodName, isRequest()));
    }

    @Override
    public void unWrapBody() {
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

    @JsonIgnore
    public void setProtoDescriptor(Optional<ProtoDescriptorDAO> protoDescriptor) {
        this.protoDescriptor = protoDescriptor;
        postParse();
    }


    @Override
    public String rawPayloadAsString(boolean wrapForDisplay) throws
        NotImplementedException, RawPayloadProcessingException {
        try {
            if (this.dataObj.isDataObjEmpty()) {
                return mapper.writeValueAsString(this);
            } else {
                if (wrapForDisplay) {
                    // Grpc body is already unwrapped decoded and ready for display just need to
                    // make string out of it and return
                }
                return dataObj.serializeDataObj();
            }
        } catch (Exception e) {
            throw  new RawPayloadProcessingException(e);
        }
    }
}
