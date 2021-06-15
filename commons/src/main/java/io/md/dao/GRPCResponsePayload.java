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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.md.constants.Constants;
import io.md.logger.LogMgr;
import io.md.utils.GPRCResponsePayloadDeserialzer;
import io.md.utils.Utils;

@JsonDeserialize(using = GPRCResponsePayloadDeserialzer.class)
public class GRPCResponsePayload extends GRPCPayload implements ResponsePayload {

	private static final Logger LOGGER = LogMgr.getLogger(GRPCResponsePayload.class);

	@JsonProperty("status")
	private Integer status;

	@JsonDeserialize(as= MultivaluedHashMap.class)
	@JsonProperty("trls")
	protected MultivaluedMap<String, String> trls;


	public GRPCResponsePayload(MultivaluedMap<String, String> hdrs, byte[] body,
		 String path, Integer status, MultivaluedMap<String, String> trls) {
		super(hdrs, body, path);
		this.status = status;
		if (trls != null) this.trls = trls;
	}

	public GRPCResponsePayload(JsonNode deserializedJsonTree) {
		super(deserializedJsonTree);
		this.trls =  this.dataObj.getValAsObject("/".concat("trls"),
			MultivaluedHashMap.class).orElse(new MultivaluedHashMap());
		try {
			Optional<Integer> optionalStatus = this.dataObj
				.getValAsObject(Constants.GRPC_STATUS_PATH,
					Integer.class);
			if(optionalStatus.isPresent()) {
				this.status = optionalStatus.get();
			} else {
				this.status = Constants.GRPC_SUCCESS_STATUS_CODE;
				LOGGER.debug("grpc-status trailer not present in response dataobj. Setting status as 0");
			}

			((ObjectNode)this.dataObj.getRoot()).set("status"
				, JsonNodeFactory.instance.numberNode(this.status));

		}  catch (Exception e) {
			LOGGER.error("Unable to set status in GRPC Response payload " , e);
		}

	}

	@Override
	boolean isRequest() {
		return false;
	}

	@Override
	public String getStatusCode() {
		return String.valueOf(this.status);
	}

	@JsonIgnore
	public MultivaluedMap<String, String> getTrls() {
		if (this.dataObj != null && !this.dataObj.isDataObjEmpty()) {
			return this.dataObj.getValAsObject("/".concat("trls"),
				MultivaluedHashMap.class).orElse(new MultivaluedHashMap<>());
		}
		return trls;
	}

}
