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

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;

import io.md.utils.JsonPayloadDeserializer;

@JsonDeserialize(using = JsonPayloadDeserializer.class)
public class JsonPayload extends LazyParseAbstractPayload {

	@JsonSerialize(using = StringSerializer.class)
	public String json;


	public JsonPayload(@JsonProperty("json") String payload) {
		this.json = payload;
	}

	public JsonPayload(JsonNode dataObjRoot) {
		super(dataObjRoot);
	}

	@Override
	public byte[] rawPayloadAsByteArray() throws RawPayloadEmptyException {
		if (isRawPayloadEmpty()) {
			throw new RawPayloadEmptyException("Payload String Empty/Null");
		}
		return json.getBytes();
	}

	@Override
	public String rawPayloadAsString() throws RawPayloadEmptyException {
		return rawPayloadAsString(false);
	}

	@Override
	public String rawPayloadAsString(boolean wrapForDisplay)
		throws NotImplementedException, RawPayloadEmptyException {
		if (isRawPayloadEmpty()) {
			throw new RawPayloadEmptyException("Payload String Empty/Null");
		}
		return this.json;
	}

	@Override
	public void parseIfRequired(){
		if (this.dataObj == null) {
			this.dataObj = new JsonDataObj(json, mapper);
		}
	}

	@Override
	public void postParse() {
		// DO NOTHING (No unwrapping required)
	}

	@Override
	public boolean isRawPayloadEmpty() {
		return (json == null || json.isEmpty()) && this.dataObj.isDataObjEmpty();
	}
}
