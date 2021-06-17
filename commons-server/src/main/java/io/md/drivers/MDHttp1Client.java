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

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;
import io.md.dao.RequestDetails;

public class MDHttp1Client extends MDHttpClient {


	private final HttpClient client;

	public MDHttp1Client(HttpClient client , RequestDetails details){
		super(details);
		this.client =client;

	}

	private HttpRequest buildRequest(){
		HttpRequest.Builder reqbuilder = HttpRequest.newBuilder()
			.uri(uri)
			.method(method,
				HttpRequest.BodyPublishers.ofByteArray(body));


		headers.forEach((k, vlist) -> {
			vlist.forEach(value -> reqbuilder.header(k, value));
		});
		return reqbuilder.build();
	}

	@Override
	public MDResponse makeRequest() throws Exception {
		HttpRequest request = buildRequest();
		HttpResponse<byte[]> response = client
			.send(request, BodyHandlers.ofByteArray());
		return new MDHttpResponse(response);
	}

	@Override
	public CompletableFuture<MDResponse> makeRequestAsync() {
		HttpRequest request = buildRequest();
		CompletableFuture<HttpResponse<byte[]>> httpResponse = client
			.sendAsync(request, BodyHandlers.ofByteArray());
		return httpResponse.thenApply(resp->new MDHttpResponse(resp));
	}
}
