/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.drivers;

import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.glassfish.jersey.uri.UriComponent;

import io.cube.agent.UtilException;

import com.cube.core.Utils;
import com.cube.dao.Event;
import com.cube.dao.HTTPRequestPayload;
import com.cube.dao.Replay;
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
		public int send(IReplayRequest request) throws IOException, InterruptedException {
			return httpClient
				.send(((HttpReplayRequest) request).request, HttpResponse.BodyHandlers.discarding())
				.statusCode();
		}

		@Override
		public CompletableFuture<Integer> sendAsync(IReplayRequest request) {
			return httpClient.sendAsync(((HttpReplayRequest) request).request,
				HttpResponse.BodyHandlers.discarding())
				.thenApply(HttpResponse::statusCode).handle((ret, e) -> {
					if (e != null) {
						LOGGER.error(
							new ObjectMessage(
								Map.of(Constants.MESSAGE, "Exception in replaying requests")),
							e);
						return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
					}
					return ret;
				});
		}

		@Override
		public IReplayRequest build(Replay replay, Event reqEvent, Config config)
			throws IOException {
			HTTPRequestPayload httpRequest = Utils.getRequestPayload(reqEvent, config);

			// transform fields in the request before the replay.
			replay.xfmer.ifPresent(x -> x.transformRequest(httpRequest));

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
					HttpRequest.BodyPublishers.ofString(httpRequest.body));

			httpRequest.hdrs.forEach((k, vlist) -> {
				// some headers are restricted and cannot be set on the request
				// lua adds ':' to some headers which we filter as they are invalid
				// and not needed for our requests.
				if (Utils.ALLOWED_HEADERS.test(k) && !k.startsWith(":")) {
					vlist.forEach(value -> reqbuilder.header(k, value));
				}
			});

			return new HttpReplayRequest(reqbuilder.build());

		}

		@Override
		public int getSuccessStatusCode() {
			return Response.Status.OK.getStatusCode();
		}

		@Override
		public int getErrorStatusCode() {
			return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
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
