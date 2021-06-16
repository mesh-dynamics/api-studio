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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

public interface RawPayload {

	@JsonIgnore
	public byte[] rawPayloadAsByteArray() throws NotImplementedException, RawPayloadEmptyException;

	@JsonIgnore
	public String rawPayloadAsString() throws NotImplementedException, RawPayloadEmptyException, RawPayloadProcessingException;

	@JsonIgnore
	public String rawPayloadAsString(boolean wrapForDisplay) throws
		NotImplementedException, RawPayloadEmptyException, RawPayloadProcessingException;


	@JsonIgnore
	public boolean isRawPayloadEmpty();

	static class RawPayloadProcessingException extends Exception {
		public RawPayloadProcessingException(Throwable e) {
			super(e);
		}

		public RawPayloadProcessingException(String msg) {
			super(msg);
		}

		public RawPayloadProcessingException(String msg, Throwable e) {
			super(msg, e);
		}
	}

	static class RawPayloadEmptyException extends Exception {

		public RawPayloadEmptyException(Throwable e) {
			super(e);
		}

		public RawPayloadEmptyException(String msg) {
			super(msg);
		}

		public RawPayloadEmptyException(String msg, Throwable e) {
			super(msg, e);
		}
	}

}
