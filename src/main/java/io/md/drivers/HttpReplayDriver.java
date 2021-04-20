/*
 *
 *    Copyright Cube I O
 *
 */

package io.md.drivers;

import java.io.IOException;
import java.net.Authenticator;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.core.RRTransformerOperations;
import io.md.core.Utils;
import io.md.dao.Event;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.RRTransformer;
import io.md.dao.Replay;
import io.md.dao.RequestDetails;
import io.md.dao.RequestPayload;
import io.md.dao.ResponsePayload;
import io.md.services.DataStore;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-08
 * @author Prasad M D
 */
public class HttpReplayDriver extends AbstractReplayDriver {

	private static Logger LOGGER = LogManager.getLogger(HttpReplayDriver.class);

	protected final ObjectMapper jsonMapper;

	HttpReplayDriver(Replay replay, DataStore dataStore, ObjectMapper jsonMapper) {
		super(replay, dataStore);
		this.jsonMapper = jsonMapper;
	}

	@Override
	public IReplayClient initClient(Replay replay) throws Exception {
		return new HttpReplayClient(replay, jsonMapper);
	}

	@Override
	protected void modifyResponse(Event respEvent) {
		return;
	}


	static class HttpReplayClient extends AbstractIReplayClient{

		protected HttpClient httpClient;
		protected Optional<RRTransformer> xfmer = Optional.empty();
		protected final ObjectMapper jsonMapper;

		HttpReplayClient(Replay replay, ObjectMapper jsonMapper) throws Exception {
			HttpClient.Builder clientbuilder = HttpClient.newBuilder()
				.version(Version.HTTP_2) // As per the docs:
				// The Java HTTP Client supports both HTTP/1.1 and HTTP/2. By default the client
				// will send requests using HTTP/2. Requests sent to servers that do not yet support
				// HTTP/2 will automatically be downgraded to HTTP/1.1.

				// Outdated comment below (can remove after testing version HTTP_2)
				// need to explicitly set this (version as 1.1)
				// if server is not supporting HTTP 2.0, getting a 403 error

				//.followRedirects(HttpClient.Redirect.NORMAL)  // Don't follow redirects
				.connectTimeout(Duration.ofSeconds(20));
			if (Authenticator.getDefault() != null) {
				clientbuilder.authenticator(Authenticator.getDefault());
			}
			httpClient = clientbuilder.build();
			replay.xfms.ifPresent(xfms -> {
				JSONObject obj = new JSONObject(xfms);
				this.xfmer = Optional.of(new RRTransformer(obj));
			});
			this.jsonMapper = jsonMapper;
		}

		@Override
		public ResponsePayload formResponsePayload(MDResponse response) {

			return new HTTPResponsePayload(response.getHeaders(), response.statusCode(), response.getBody());
		}

		@Override
		public boolean verifyPayload(Event reqEvent) throws IOException {
			return reqEvent.payload instanceof HTTPRequestPayload;
		}

		@Override
		MDHttpClient getClient(RequestDetails details) {
			return new MDHttp1Client(httpClient , details);
		}

		@Override
		public RequestPayload modifyRequest(Event reqEvent) {
			HTTPRequestPayload httpRequest = (HTTPRequestPayload) reqEvent.payload;

			// TODO: Replace transformations functionality using injection
			// transform fields in the request before the replay.
			xfmer.ifPresent(x -> RRTransformerOperations.transformRequest(httpRequest, x, jsonMapper));
			return httpRequest;
		}
	}

	static class HttpReplayRequest implements IReplayRequest {

		public HttpRequest request;

		HttpReplayRequest(HttpRequest request) {
			this.request = request;
		}

	}

}
