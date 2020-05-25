/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.drivers;

import com.cube.core.RRTransformerOperations;

import io.md.dao.HTTPResponsePayload;
import io.md.dao.Replay;
import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.uri.UriComponent;

import io.cube.agent.UtilException;
import io.md.dao.Event;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.ResponsePayload;

import com.cube.core.Utils;
import com.cube.utils.Constants;
import com.cube.ws.Config;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-08
 * @author Prasad M D
 */
public class HttpReplayDriver extends AbstractReplayDriver {

	private static Logger LOGGER = LogManager.getLogger(HttpReplayDriver.class);

	HttpReplayDriver(Replay replay, Config config) {
		super(replay, config);
	}

	@Override
	public IReplayClient initClient(Replay replay) throws Exception {
		return new HttpReplayClient();
	}


	static class HttpReplayClient implements IReplayClient {

		private HttpClient httpClient;

		HttpReplayClient() throws Exception {
			HttpClient.Builder clientbuilder = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1) // need to explicitly set this
				// if server is not supporting HTTP 2.0, getting a 403 error
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(20));
			if (Authenticator.getDefault() != null) {
				clientbuilder.authenticator(Authenticator.getDefault());
			}
			httpClient = clientbuilder.build();
		}

		@Override
		public ResponsePayload send(Event requestEvent, Replay replay)
			throws IOException, InterruptedException {
			HttpRequest request = build(replay, requestEvent);
			HttpResponse<byte[]> response = httpClient
				.send(request, HttpResponse.BodyHandlers.ofByteArray());
			return formResponsePayload(response);
		}

		private ResponsePayload formResponsePayload(HttpResponse<byte[]> response) {
				byte[] responseBody = response.body();
				MultivaluedMap<String, String> responseHeaders = new MultivaluedHashMap<>();
				response.headers().map().forEach((k, v) -> {
					responseHeaders.addAll(k, v);
				});
				HTTPResponsePayload responsePayload = new HTTPResponsePayload(responseHeaders,
					response.statusCode(), responseBody);
			return responsePayload;
		}

		@Override
		public CompletableFuture<ResponsePayload> sendAsync(Event requestEvent, Replay replay)
			 {
				 HttpRequest request = null;
				 try {
					 request = build(replay, requestEvent);
				 } catch (IOException e) {
					 e.printStackTrace();
				 }
				 CompletableFuture<HttpResponse<byte[]>> httpResponse = httpClient
				.sendAsync(request, BodyHandlers.ofByteArray());
			return httpResponse.thenApply(response -> {
				return formResponsePayload(response);
				});
			 }

		public HttpRequest build(Replay replay, Event reqEvent)
			throws IOException {
			if (!(reqEvent.payload instanceof  HTTPRequestPayload)) {
				throw new IOException("Invalid Payload type");
			}
			HTTPRequestPayload httpRequest =  (HTTPRequestPayload) reqEvent.payload;

			// transform fields in the request before the replay.
			replay.xfmer.ifPresent(x -> RRTransformerOperations.transformRequest(httpRequest, x));

			UriBuilder uribuilder = UriBuilder.fromUri(replay.endpoint)
				.path(reqEvent.apiPath);
			httpRequest.queryParams.forEach(UtilException.rethrowBiConsumer((k, vlist) -> {
				String[] params = vlist.stream().map(UtilException.rethrowFunction(v -> {
					return UriComponent
						.encode(v, UriComponent.Type.QUERY_PARAM_SPACE_ENCODED);
					// return URLEncoder.encode(v, "UTF-8"); // this had a problem of
					// encoding space as +, which further gets encoded as %2B
				})).toArray(String[]::new);
				uribuilder.queryParam(k, (Object[]) params);
			}));
			URI uri = uribuilder.build();
			HttpRequest.Builder reqbuilder = HttpRequest.newBuilder()
				.uri(uri)
				.method(httpRequest.method,
					HttpRequest.BodyPublishers.ofByteArray(httpRequest.getBody()));

			httpRequest.hdrs.forEach((k, vlist) -> {
				// some headers are restricted and cannot be set on the request
				// lua adds ':' to some headers which we filter as they are invalid
				// and not needed for our requests.
				if (Utils.ALLOWED_HEADERS.test(k) && !k.startsWith(":")) {
					vlist.forEach(value -> reqbuilder.header(k, value));
				}
			});

			//Adding additional headers during Replay, This will help identify the case where the request is retried
            // by the platform for some reason, which leads to multiple identical events during the replay run.
            reqbuilder.header(Constants.CUBE_HEADER_PREFIX + Constants.SRC_REQUEST_ID, reqEvent.reqId);

            //This will help to catch if the same request is replayed multiple times by Replay Driver
            reqbuilder.header(Constants.CUBE_HEADER_PREFIX + Constants.REQUEST_ID, UUID.randomUUID().toString());

			return reqbuilder.build();
		}

		@Override
		public boolean isSuccessStatusCode(String responseCode) {
			Optional<Integer> intResponse = Utils.strToInt(responseCode);
			return intResponse.map(intCode -> {
				if(Response.Status.Family.familyOf(intCode)
					.equals(Response.Status.Family.SUCCESSFUL)) {
					return true;
				}
				return false;
			}).orElse(false);
		}

		@Override
		public String getErrorStatusCode() {
			return String.valueOf(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
		}

		@Override
		public boolean tearDown() {
			//do nothing
			return true;
		}
	}

	static class HttpReplayRequest implements IReplayRequest {

		public HttpRequest request;

		HttpReplayRequest(HttpRequest request) {
			this.request = request;
		}

	}

}
