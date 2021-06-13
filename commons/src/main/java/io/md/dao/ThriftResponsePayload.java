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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.md.core.Comparator.MatchType;
import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;
import io.md.cryptography.EncryptionAlgorithm;
import io.md.utils.Utils;

//Dummy implementation only status works.
// TODO: Complete this later.
public class ThriftResponsePayload extends  LazyParseAbstractPayload implements ResponsePayload  {

	public int status;

	public ThriftResponsePayload( int status) {
		this.status = status;
	}

	@Override
	public String getStatusCode() {
		return String.valueOf(status);
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
	public void postParse() {}

	public  void replaceContent(List<String> pathsToKeep, String path) {
		return;
	}

	@Override
	public long size() {
		return 0;
	}

	@Override
	public void updatePayloadBody() throws PathNotFoundException {
		return;
	}
}
