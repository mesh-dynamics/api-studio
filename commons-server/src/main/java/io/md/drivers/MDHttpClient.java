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

package io.md.drivers;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import io.md.dao.RequestDetails;


public abstract class MDHttpClient {
	public final URI uri;

	public final String method;

	public final byte[] body;

	public final MultivaluedMap<String , String> headers;


	public MDHttpClient(RequestDetails details){
		this(details.uri , details.method, details.body, details.headers);
	}

	public MDHttpClient(URI uri , String method, byte[] body , MultivaluedMap<String , String> headers){
		this.uri = uri;
		this.method = method.toUpperCase();
		this.body = body;
		this.headers = headers!=null ? headers : new MultivaluedHashMap<>();
	}

	public abstract MDResponse makeRequest() throws Exception;

	public abstract CompletableFuture<MDResponse> makeRequestAsync();
}

