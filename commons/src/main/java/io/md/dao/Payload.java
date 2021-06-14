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

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME,
	property = "type")
@JsonSubTypes({
	@Type(value = HTTPRequestPayload.class),
	@Type(value = HTTPResponsePayload.class),
	@Type(value = JsonPayload.class),
	@Type(value = JsonByteArrayPayload.class),
	@Type(value = FnReqRespPayload.class),
	@Type(value = GRPCRequestPayload.class),
	@Type(value = GRPCResponsePayload.class)
})
public interface Payload extends DataObj, RawPayload {

	Payload applyTransform(Payload rhs, List<ReqRespUpdateOperation> operationList);
	long size();
	void updatePayloadBody() throws PathNotFoundException;
	void replaceContent(List<String> pathsToKeep, String path, long maxSize);
	String getPayloadAsJsonString();
	ConvertEventPayloadResponse checkAndConvertResponseToString(boolean wrapForDisplay
		, List<String> pathsToKeep, long size, String path);
	String getPayloadAsJsonString(boolean wrapForDisplay);
	@JsonIgnore
	default  List<String> getPayloadFields(){
		return Collections.EMPTY_LIST;
	}

}
