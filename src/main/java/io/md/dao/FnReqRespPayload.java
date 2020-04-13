package io.md.dao;

import java.time.Instant;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.md.utils.FnReqRespPayloadDeserializer;

@JsonDeserialize(using = FnReqRespPayloadDeserializer.class)
public class FnReqRespPayload extends LazyParseAbstractPayload {


	@JsonProperty("respTs")
	public  Optional<Instant> respTS;
	//public final  Integer[] argsHash;
	@JsonProperty("argVals")
	public   Object[] argVals;
	@JsonProperty("retOrExceptionVal")
	public   Object retOrExceptionVal;
	@JsonProperty("retStatus")
	public   RetStatus retStatus;
	@JsonProperty("exceptionType")
	public   Optional<String> exceptionType;

	public enum RetStatus {
		Success,
		Exception
	}

	public FnReqRespPayload(Optional<Instant> respTs, Object[] args, Object retOrException,
		RetStatus retStatus, Optional<String> exceptionType) {
		this.respTS = respTs;
		this.argVals = args;
		this.retOrExceptionVal = retOrException;
		this.retStatus = retStatus;
		this.exceptionType = exceptionType;
	}

	public FnReqRespPayload(JsonNode deserializedJsonTree) {
		super(deserializedJsonTree);
		/*this.queryParams = this.dataObj.getValAsObject("/".concat("queryParams"),
			MultivaluedHashMap.class).orElse(new MultivaluedHashMap<>());
		*/
		postParse();
	}



	@Override
	public byte[] rawPayloadAsByteArray() throws NotImplementedException, RawPayloadEmptyException {
		return new byte[0];
	}

	@Override
	public String rawPayloadAsString()
		throws NotImplementedException, RawPayloadProcessingException, RawPayloadEmptyException {
		return null;
	}

	@Override
	public String rawPayloadAsString(boolean wrapForDisplay)
		throws NotImplementedException, RawPayloadEmptyException, RawPayloadProcessingException {
		return null;
	}

	@Override
	public boolean isRawPayloadEmpty() {
		return false;
	}

	@Override
	public void postParse() {

	}

}
