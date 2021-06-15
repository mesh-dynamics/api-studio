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
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.async.MinimalHttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.AsyncClientEndpoint;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.config.H2Config;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.md.dao.RequestDetails;

public class MDHttp2Client extends MDHttpClient{

	public static class DummyCB implements FutureCallback<SimpleHttpResponse> {

		@Override
		public void completed(SimpleHttpResponse simpleHttpResponse) {}

		@Override
		public void failed(Exception e) {}

		@Override
		public void cancelled() {}
	}


	private static Logger LOGGER = LogManager.getLogger(MDHttp2Client.class);

	public MDHttp2Client(URI uri , String method, byte[] body , MultivaluedMap<String , String> headers ){
		super(uri , method , body , headers);
	}
	public MDHttp2Client(RequestDetails details){
		super(details);
	}

	private SimpleHttpRequest buildRequest(){
		SimpleHttpRequest simpleHttpRequest = new SimpleHttpRequest(method , uri);
		if(body!=null) simpleHttpRequest.setBody(body , null);

		headers.forEach((k, vlist) -> {
			vlist.forEach(value -> simpleHttpRequest.setHeader(k, value));
		});
		return simpleHttpRequest;
	}

	public MDResponse makeRequest() throws Exception {

		SimpleHttpRequest simpleHttpRequest = buildRequest();

		return new MDHttp2Response(getSimpleHttpResponse(uri , simpleHttpRequest), uri.getPath(), MDHttp2Response.EMPTY);
	}

	@Override
	public CompletableFuture<MDResponse> makeRequestAsync(){
		try{
			return CompletableFuture.completedFuture(makeRequest());
		}catch (Exception e){
			LOGGER.error("send async error " , e);
			throw new CompletionException(e);
		}
	}

	public static SimpleHttpResponse getSimpleHttpResponse(URI uri , SimpleHttpRequest request)
		throws InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		final IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
			.setSoTimeout(Timeout.ofSeconds(500))
			.build();

		final MinimalHttpAsyncClient client = HttpAsyncClients.createMinimal(
			HttpVersionPolicy.FORCE_HTTP_2, H2Config.DEFAULT, null, ioReactorConfig);

		client.start();

		URI targetURI = new URI(String.format("%s://%s" , uri.getScheme() , uri.getAuthority()));
		final HttpHost target = HttpHost.create(targetURI);
		LOGGER.debug("connecting to target host "+targetURI);

		final Future<AsyncClientEndpoint> leaseFuture = client.lease(target, null);
		AsyncClientEndpoint endpoint = leaseFuture.get(30, TimeUnit.SECONDS);

		LOGGER.debug("connected to end point making request "+request);

		Future<SimpleHttpResponse> responseFuture = endpoint.execute(
			SimpleRequestProducer.create(request),
			SimpleResponseConsumer.create(),
			new DummyCB());

		SimpleHttpResponse resp = responseFuture.get();

		LOGGER.info("received response "+resp + " releasing the endpoint resources and closing connection");

		endpoint.releaseAndReuse();

		client.close(CloseMode.GRACEFUL);
		return resp;
	}

}
