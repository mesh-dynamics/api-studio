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

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;

import io.md.utils.JsonByteArrayPayloadDeserializer;

@JsonDeserialize(using = JsonByteArrayPayloadDeserializer.class)
public class JsonByteArrayPayload extends LazyParseAbstractPayload {

	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
	public byte[] jsonBinary;


	public JsonByteArrayPayload(@JsonProperty("jsonBinary") byte[] payload) {
		this.jsonBinary = payload;
	}

	public JsonByteArrayPayload(JsonNode dataObjRoot) {
		super(dataObjRoot);
	}

	@Override
	public byte[] rawPayloadAsByteArray() throws RawPayloadEmptyException  {
		if (this.isRawPayloadEmpty()) {
			throw new RawPayloadEmptyException("Byte Array is Empty/Null");
		}
		return jsonBinary;
	}

	@Override
	public String rawPayloadAsString() throws RawPayloadEmptyException {
		return rawPayloadAsString(false);
	}

	@Override
	public String rawPayloadAsString(boolean wrapForDisplay)
		throws NotImplementedException, RawPayloadEmptyException {
		if (this.isRawPayloadEmpty()) {
			throw new RawPayloadEmptyException("Byte Array is Empty/Null");
		}
		return new String(jsonBinary, StandardCharsets.UTF_8);
	}

	@Override
	public boolean isRawPayloadEmpty() {
		return (this.jsonBinary == null || this.jsonBinary.length == 0) && this.dataObj.isDataObjEmpty();
	}

	@Override
	public void parseIfRequired(){
		if (this.dataObj == null) {
			this.dataObj = new JsonDataObj(jsonBinary, mapper);
		}
	}

	@Override
	public void postParse() {
		//Do Nothing (No unwrapping required)
	}
}
