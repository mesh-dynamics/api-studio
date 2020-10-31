/*
 *
 *    Copyright Cube I O
 *
 */

package io.md.drivers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.uri.UriComponent;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.dao.RequestPayload;
import io.md.utils.UtilException;
import io.md.core.RRTransformerOperations;
import io.md.core.Utils;
import io.md.dao.Event;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.RRTransformer;
import io.md.dao.Replay;
import io.md.dao.ResponsePayload;
import io.md.services.DataStore;
import io.md.utils.Constants;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-08
 * @author Prasad M D
 */
public class HttpReplayDriver extends AbstractReplayDriver {

	private static Logger LOGGER = LogManager.getLogger(AbstractReplayDriver.class);

	protected final ObjectMapper jsonMapper;

	HttpReplayDriver(Replay replay, DataStore dataStore, ObjectMapper jsonMapper) {
		super(replay, dataStore);
		this.jsonMapper = jsonMapper;
	}

	@Override
	public IReplayClient initClient(Replay replay) throws Exception {
		return new HttpReplayClient(replay, jsonMapper);
	}


	static class HttpReplayClient implements IReplayClient {

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
		public ResponsePayload send(Event requestEvent, Replay replay)
			throws IOException, InterruptedException {
			HttpRequest request = build(replay, requestEvent);
			HttpResponse<byte[]> response = httpClient
				.send(request, BodyHandlers.ofByteArray());
			return formResponsePayload(response);
		}

		public static InputStream getDecodedInputStream(InputStream body, HttpHeaders headers) {
			String encoding = determineContentEncoding(headers);
			try {
				switch (encoding) {
					case "":
						return body;
					case "gzip":
						return new GZIPInputStream(body);
					default:
						throw new UnsupportedOperationException(
								"Unexpected Content-Encoding: " + encoding);
				}
			} catch (IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
		}

		public static String determineContentEncoding(
				HttpHeaders headers) {
			return headers.firstValue("Content-Encoding").orElse("");
		}

		private ResponsePayload formResponsePayload(HttpResponse<byte[]> response)
		{

			byte[] originalBody = response.body();
			InputStream stream = getDecodedInputStream(new ByteArrayInputStream(originalBody), response.headers());
			byte[] responseBody = originalBody;
			try {
				responseBody = stream.readAllBytes();
			} catch (IOException e) {
				responseBody = response.body();
				e.printStackTrace();
			}

			MultivaluedMap<String, String> responseHeaders = new MultivaluedHashMap<>();
			response.headers().map().forEach((k, v) -> {
				responseHeaders.addAll(k, v);
			});
			HTTPResponsePayload responsePayload = new HTTPResponsePayload(responseHeaders,
					response.statusCode(), responseBody);
			return responsePayload;
		}

		@Override
		public CompletableFuture<ResponsePayload> sendAsync(Event requestEvent, Replay replay) {
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

		protected boolean verifyPayload(Event reqEvent) throws IOException {
			return reqEvent.payload instanceof HTTPRequestPayload;
		}

		protected RequestPayload modifyRequest(Event reqEvent) {
			HTTPRequestPayload httpRequest = (HTTPRequestPayload) reqEvent.payload;

			// TODO: Replace transformations functionality using injection
			// transform fields in the request before the replay.
			xfmer.ifPresent(x -> RRTransformerOperations.transformRequest(httpRequest, x, jsonMapper));
			return httpRequest;
		}

		public HttpRequest build(Replay replay, Event reqEvent)
			throws IOException {
			if (!verifyPayload(reqEvent)) {
				throw new IOException("Invalid Payload type");
			}
			RequestPayload httpRequest = modifyRequest(reqEvent);
			return buildRequest(replay, reqEvent, httpRequest);
		}

		protected HttpRequest buildRequest(Replay replay, Event reqEvent,
			RequestPayload httpRequest) {

			List<String> pathSegments = httpRequest
				.getValAsObject(Constants.PATH_SEGMENTS_PATH, List.class)
				.orElse(Collections.EMPTY_LIST);

			String apiPath;
			if(!pathSegments.isEmpty())
			{
				try {
					apiPath = pathSegments.stream().collect(Collectors.joining("/"));
				} catch (Exception e) {
					LOGGER.error("Cannot form apiPath from pathSegments. Resolving to event apiPath", e);
					apiPath = reqEvent.apiPath;
				}
			} else {
				LOGGER.error("pathSegments not found. Resolving to event apiPath");
				apiPath = reqEvent.apiPath;
			}

			UriBuilder uribuilder = UriBuilder.fromUri(replay.endpoint)
				.path(apiPath);

			byte[] requestBody = httpRequest.getBody();
			URI uri = uribuilder.build();
			HttpRequest.Builder reqbuilder = HttpRequest.newBuilder()
				.uri(uri)
				.method(httpRequest.getMethod(),
					HttpRequest.BodyPublishers.ofByteArray(requestBody));

			LOGGER.debug("PATH :: " + uri.toString() + " OUTGOING REQUEST BODY :: " + new String(requestBody,
				StandardCharsets.UTF_8));

			// Fetch headers/queryParams and path etc from payload since injected value
			// would be present in dataObj instead of payload fields

			//TODO - HTTPHeaders and queryParams don't support types
			// they have to be string so add validation for that,
			// also add validation to be an array even if a singleton
			// because of jackson serialisation to Multivalued map
			
			// NOTE - HEADERS SHOULD BE READ AND SET AFTER SETTING THE BODY BECAUSE WHILE DOING GETBODY()
			// THE HEADERS MIGHT GET UPDATED ESPECIALLY IN CASE OF MULTIPART DATA WHERE WE SET NEW CONTENT-TYPE
			// HEADER WHILE WRAPPING THE BODY

			MultivaluedHashMap<String, String> headers = httpRequest
				.getValAsObject(Constants.HDR_PATH, MultivaluedHashMap.class)
				.orElse(new MultivaluedHashMap<String, String>());

			MultivaluedHashMap<String, String> queryParams = httpRequest
				.getValAsObject(Constants.QUERY_PARAMS_PATH, MultivaluedHashMap.class)
				.orElse(new MultivaluedHashMap<String, String>());

			headers.forEach((k, vlist) -> {
				// some headers are restricted and cannot be set on the request
				// lua adds ':' to some headers which we filter as they are invalid
				// and not needed for our requests.
				if (Utils.ALLOWED_HEADERS.test(k) && !k.startsWith(":")) {
					vlist.forEach(value -> reqbuilder.header(k, value));
				}
			});

			queryParams.forEach(UtilException.rethrowBiConsumer((k, vlist) -> {
				String[] params = vlist.stream().map(UtilException.rethrowFunction(v -> {
					return UriComponent
						.encode(v, UriComponent.Type.QUERY_PARAM_SPACE_ENCODED);
					// return URLEncoder.encode(v, "UTF-8"); // this had a problem of
					// encoding space as +, which further gets encoded as %2B
				})).toArray(String[]::new);
				uribuilder.queryParam(k, (Object[]) params);
			}));


			//Adding additional headers during Replay, This will help identify the case where the request is retried
			// by the platform for some reason, which leads to multiple identical events during the replay run.
			reqbuilder
				.header(Constants.CUBE_HEADER_PREFIX + Constants.SRC_REQUEST_ID, reqEvent.reqId);

			//This will help to catch if the same request is replayed multiple times by Replay Driver
			reqbuilder.header(Constants.CUBE_HEADER_PREFIX + Constants.REQUEST_ID,
				UUID.randomUUID().toString());

			return reqbuilder.build();
		}

		@Override
		public boolean isSuccessStatusCode(String responseCode) {
			Optional<Integer> intResponse = Utils.strToInt(responseCode);
			return intResponse.map(intCode -> {
				if (Response.Status.Family.familyOf(intCode)
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
