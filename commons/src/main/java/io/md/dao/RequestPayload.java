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

import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.JsonNode;

import io.md.constants.Constants;

public interface RequestPayload extends Payload {

	public String getMethod();

	public byte[] getBody();

	public MultivaluedMap<String, String> getQueryParams();

	default List<String> getPayloadFields() {
		return Arrays.asList(String.format("%s:%s", Constants.METHOD_PATH, getMethod()));
	}
}
