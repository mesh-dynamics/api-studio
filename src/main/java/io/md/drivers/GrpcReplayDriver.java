package io.md.drivers;

import static io.md.utils.Utils.ALLOWED_HEADERS;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriBuilder;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.uri.UriComponent;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.cache.ProtoDescriptorCache;
import io.md.dao.Event;
import io.md.dao.GRPCRequestPayload;
import io.md.dao.GRPCResponsePayload;
import io.md.dao.Replay;
import io.md.dao.RequestPayload;
import io.md.dao.ResponsePayload;
import io.md.services.DataStore;
import io.md.utils.Constants;
import io.md.utils.UtilException;
import io.md.utils.Utils;

public class GrpcReplayDriver extends HttpReplayDriver {

	protected final Optional<ProtoDescriptorCache> protoDescriptorCacheOptional;

	private static Logger LOGGER = LogManager.getLogger(GrpcReplayDriver.class);


	GrpcReplayDriver(Replay replay, DataStore dataStore,
		ObjectMapper jsonMapper, Optional<ProtoDescriptorCache> protoDescriptorCacheOptional) {
		super(replay, dataStore, jsonMapper);
		this.protoDescriptorCacheOptional = protoDescriptorCacheOptional;
	}

	@Override
	public IReplayClient initClient(Replay replay) throws Exception {
		return new GrpcReplayClient(replay, jsonMapper, protoDescriptorCacheOptional);
	}

	static class GrpcReplayClient extends HttpReplayClient {

		protected final Optional<ProtoDescriptorCache> protoDescriptorCacheOptional;


		GrpcReplayClient(Replay replay, ObjectMapper jsonMapper, Optional<ProtoDescriptorCache> protoDescriptorCacheOptional) throws Exception {
			super(replay, jsonMapper);
			this.protoDescriptorCacheOptional = protoDescriptorCacheOptional;
		}

		@Override
		protected boolean verifyPayload(Event reqEvent) throws IOException {
			return reqEvent.payload instanceof GRPCRequestPayload;
		}

		@Override
		protected RequestPayload modifyRequest(Event reqEvent) {
			try {

				protoDescriptorCacheOptional.map(
					protoDescriptorCache -> {
						io.md.utils.Utils.setProtoDescriptorGrpcEvent(reqEvent, protoDescriptorCache);
						return protoDescriptorCache;
					}
				).orElseThrow(() -> new Exception(
					"protoDescriptorCache is missing for GRPCPayload in GRPCReplayDriver"));
			} catch (Exception e) {
				LOGGER.error("protoDescriptorCache is missing for GRPCPayload in GRPCReplayDriver", e);
			}
			return (RequestPayload) reqEvent.payload;
		}

		@Override
		protected ResponsePayload formResponsePayload(MDResponse response)
		{
			// TODO: Currently trailers aren't supported by java HttpResponse so using
			//  http status code as response.statusCode and also cannot capture trailers
			//  so setting empty trailers
			GRPCResponsePayload responsePayload = new GRPCResponsePayload(response.getHeaders(),
				response.getBody(), response.getPath(), response.statusCode(), response.getTrailers() );
			return responsePayload;
		}

	}

	@Override
	protected void modifyResponse(Event respEvent) {
		if (respEvent.payload instanceof GRPCResponsePayload) {
			Utils.setProtoDescriptorGrpcEvent(respEvent, protoDescriptorCacheOptional.orElseThrow());
		}
	}

	public static  class Http2Client extends   HttpReplayClient{

		public static class DummyCB implements FutureCallback<SimpleHttpResponse>{

			@Override
			public void completed(SimpleHttpResponse simpleHttpResponse) {}

			@Override
			public void failed(Exception e) {}

			@Override
			public void cancelled() {}
		}

		protected final Optional<ProtoDescriptorCache> protoDescriptorCacheOptional;

		public Http2Client(Replay replay, ObjectMapper jsonMapper , Optional<ProtoDescriptorCache> protoDescriptorCacheOptional) throws Exception {
			super(replay , jsonMapper);
			this.protoDescriptorCacheOptional = protoDescriptorCacheOptional;
		}

		@Override
		protected boolean verifyPayload(Event reqEvent) throws IOException {
			return reqEvent.payload instanceof GRPCRequestPayload;
		}

		@Override
		protected RequestPayload modifyRequest(Event reqEvent) {
			try {

				protoDescriptorCacheOptional.map(
					protoDescriptorCache -> {
						io.md.utils.Utils.setProtoDescriptorGrpcEvent(reqEvent, protoDescriptorCache);
						return protoDescriptorCache;
					}
				).orElseThrow(() -> new Exception(
					"protoDescriptorCache is missing for GRPCPayload in GRPCReplayDriver"));
			} catch (Exception e) {
				LOGGER.error("protoDescriptorCache is missing for GRPCPayload in GRPCReplayDriver", e);
			}
			return (RequestPayload) reqEvent.payload;
		}

		@Override
		protected ResponsePayload formResponsePayload(MDResponse response)
		{
			GRPCResponsePayload responsePayload = new GRPCResponsePayload(response.getHeaders(),
				response.getBody(), response.getPath(), response.statusCode(), response.getTrailers() );
			return responsePayload;
		}


		@Override
		public ResponsePayload send(Event requestEvent, Replay replay) throws Exception {
			SimpleHttpRequest request = buildhttp2(replay, requestEvent);
			//We are reading only the response here.
			// when we read the trailers also for this http2 call , pass the same
			SimpleHttpResponse response = MDHttp2Client
				.getSimpleHttpResponse(new URI(replay.endpoint), request);
			return formResponsePayload(new MDHttp2Response(response, requestEvent.apiPath , MDHttp2Response.EMPTY)  );
		}

		@Override
		public CompletableFuture<ResponsePayload> sendAsync(Event requestEvent, Replay replay) {
			return null;
		}

		public SimpleHttpRequest buildhttp2(Replay replay, Event reqEvent)
			throws IOException {
			if (!verifyPayload(reqEvent)) {
				throw new IOException("Invalid Payload type");
			}
			RequestPayload httpRequest = modifyRequest(reqEvent);
			return buildHttp2Request(replay, reqEvent, httpRequest);
		}

		protected SimpleHttpRequest buildHttp2Request(Replay replay, Event reqEvent,
			RequestPayload httpRequest) {

			List<String> pathSegments = httpRequest
				.getValAsObject(Constants.PATH_SEGMENTS_PATH, List.class)
				.orElse(Collections.EMPTY_LIST);

			if (!pathSegments.isEmpty()) {
				try {
					reqEvent.apiPath = pathSegments.stream().collect(Collectors.joining("/"));
				} catch (Exception e) {
					LOGGER
						.error("Cannot form apiPath from pathSegments. Resolving to event apiPath",
							e);
				}
			} else {
				LOGGER.error("pathSegments not found. Resolving to event apiPath");
			}
			String apiPath = reqEvent.apiPath;
			UriBuilder uribuilder = UriBuilder.fromUri(replay.endpoint)
				.path(apiPath);

			MultivaluedHashMap<String, String> queryParams = httpRequest
				.getValAsObject(Constants.QUERY_PARAMS_PATH, MultivaluedHashMap.class)
				.orElse(new MultivaluedHashMap<String, String>());

			queryParams.forEach(UtilException.rethrowBiConsumer((k, vlist) -> {
				String[] params = vlist.stream().map(UtilException.rethrowFunction(v -> {
					return UriComponent
						.encode(v, UriComponent.Type.QUERY_PARAM_SPACE_ENCODED);
					// return URLEncoder.encode(v, "UTF-8"); // this had a problem of
					// encoding space as +, which further gets encoded as %2B
				})).toArray(String[]::new);
				uribuilder.queryParam(k, (Object[]) params);
			}));


			byte[] requestBody = httpRequest.getBody();
			URI uri = uribuilder.build();

			SimpleHttpRequest simpleHttpRequest = new SimpleHttpRequest(httpRequest.getMethod() , uri);
			simpleHttpRequest.setBody(requestBody , null);

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

			headers.forEach((k, vlist) -> {
				// some headers are restricted and cannot be set on the request
				// lua adds ':' to some headers which we filter as they are invalid
				// and not needed for our requests.
				if (ALLOWED_HEADERS.test(k) && !k.startsWith(":")) {
					vlist.forEach(value -> simpleHttpRequest.setHeader(k, value));
				}
			});


			//Adding additional headers during Replay, This will help identify the case where the request is retried
			// by the platform for some reason, which leads to multiple identical events during the replay run.
			simpleHttpRequest.setHeader(Constants.CUBE_HEADER_PREFIX + Constants.SRC_REQUEST_ID, reqEvent.reqId);

			//This will help to catch if the same request is replayed multiple times by Replay Driver
			simpleHttpRequest.setHeader(Constants.CUBE_HEADER_PREFIX + Constants.REQUEST_ID, UUID.randomUUID().toString());

			return simpleHttpRequest;
		}
	}
}