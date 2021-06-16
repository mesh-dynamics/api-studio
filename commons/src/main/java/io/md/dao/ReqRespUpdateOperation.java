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

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReqRespUpdateOperation {
	@JsonProperty("op")
	public OperationType operationType;
	@JsonProperty("path")
	public final String jsonpath;
	@JsonProperty("value")
	public Object value;
	@JsonProperty("eventType")
	public Type eventType;

	public enum Type {
		Request,
		Response;
	}

	public enum OperationType {

		ADD,
		REPLACE,
		REMOVE,
	}

	@JsonCreator
	public ReqRespUpdateOperation(@JsonProperty("op") OperationType operationType,
		@JsonProperty("path") String jsonpath) {
		this.operationType = operationType;
		this.jsonpath = jsonpath;
		this.value = null;
	}

	@Override
	public String toString() {
		return "ReqRespUpdateOperation{" +
			"operationType=" + operationType +
			", jsonpath='" + jsonpath + '\'' +
			", value=" + value +
			'}';
	}

	public String key() {
		return jsonpath.concat("::").concat(eventType.name());
	}
}
