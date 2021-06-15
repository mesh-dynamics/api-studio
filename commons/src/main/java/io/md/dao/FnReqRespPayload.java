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

package io.md.dao;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import io.md.logger.LogMgr;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.md.utils.CubeObjectMapperProvider;
import io.md.utils.FnReqRespPayloadDeserializer;

@JsonDeserialize(using = FnReqRespPayloadDeserializer.class)
public class FnReqRespPayload extends LazyParseAbstractPayload {

	private static final Logger LOGGER = LogMgr.getLogger(FnReqRespPayload.class);


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

	@Override
	public void parseIfRequired()  {
		if (this.dataObj == null) {
			// serialize and read it back so that args and ret val gets serialized by gson and read back
			ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();
			FnReqRespPayload fnReqRespPayload = this;
			try {
				String payloadStr = jsonMapper.writeValueAsString(this);
				fnReqRespPayload = jsonMapper
					.readValue(payloadStr, FnReqRespPayload.class);
			} catch (IOException e) {
				LOGGER.error("Exception in parsing FnReqRespPayload object", e);
				// we are ignoring the error and serializing the original object
				// if we throw exception, a lot of function signatures will change since
				// parseIfRequired is called from many place.
				// TODO: address this later if needed
				fnReqRespPayload = this;
			};
			this.dataObj = new JsonDataObj(fnReqRespPayload, mapper);
			postParse();
		}
	}
}
