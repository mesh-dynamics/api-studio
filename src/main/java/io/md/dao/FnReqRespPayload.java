package io.md.dao;

import java.time.Instant;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;

public class FnReqRespPayload extends LazyParseAbstractPayload {


	public  Optional<Instant> respTS;
	//public final  Integer[] argsHash;
	public   Object[] argVals;
	public   Object retOrExceptionVal;
	public   RetStatus retStatus;
	public   Optional<String> exceptionType;

	enum RetStatus {
		Success,
		Exception
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
