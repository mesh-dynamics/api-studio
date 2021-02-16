package com.cube.ws;

import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
import io.md.logger.LogMgr;
import io.md.utils.Utils;

@Path("/")
public class ProxyWS {

	@Context
	private HttpServletRequest httpServletRequest;

	@Context
	private HttpServletResponse httpServletResponse;

	private static final Logger LOGGER = LogMgr.getLogger(ProxyWS.class);

	@Path("{any: .*}")
	@GET
	public Response handleMockGet(@Context UriInfo uriInfo) {
		URI uri = formMockURL(uriInfo);
		return getResponse(uri, new byte[0]);
	}

	@Path("{any: .*}")
	@POST
	@Consumes(MediaType.WILDCARD)
	public Response handleMockPost(@Context UriInfo uriInfo, byte[] requestBody) {
		URI uri = formMockURL(uriInfo);
		return getResponse(uri, requestBody);
	}

	@Path("{any: .*}")
	@PUT
	@Consumes(MediaType.WILDCARD)
	public Response handleMockPut(@Context UriInfo uriInfo, byte[] requestBody) {
		URI uri = formMockURL(uriInfo);
		return getResponse(uri, requestBody);
	}

	@Path("{any: .*}")
	@PATCH
	@Consumes(MediaType.WILDCARD)
	public Response handleMockPatch(@Context UriInfo uriInfo, byte[] requestBody) {
		URI uri = formMockURL(uriInfo);
		return getResponse(uri, requestBody);
	}

	@Path("{any: .*}")
	@DELETE
	@Consumes(MediaType.WILDCARD)
	public Response handleMockDelete(@Context UriInfo uriInfo, byte[] requestBody) {
		URI uri = formMockURL(uriInfo);
		return getResponse(uri, requestBody);
	}

	@Path("{any: .*}")
	@OPTIONS
	@Consumes(MediaType.WILDCARD)
	public Response handleMockOptions(@Context UriInfo uriInfo, byte[] requestBody) {
		URI uri = formMockURL(uriInfo);
		return getResponse(uri, requestBody);
	}

	@Path("{any: .*}")
	@HEAD
	@Consumes(MediaType.WILDCARD)
	public Response handleMockHead(@Context UriInfo uriInfo, byte[] requestBody) {
		URI uri = formMockURL(uriInfo);
		return getResponse(uri, requestBody);
	}

	private URI formMockURL(@Context UriInfo uriInfo) {
		String customerId = CommonConfig.customerId;
		String app = CommonConfig.app;
		String instanceId = CommonConfig.instance;

		String url = uriInfo.getPath();
		String query = httpServletRequest.getQueryString();
		String reqString = url + (query == null ? "" : "?" + query);

		return UriBuilder.fromPath(CommonConfig.getInstance().CUBE_MOCK_SERVICE_URI)
			.segment("ms", customerId, app, instanceId, "*") //sending *, as service is unknown.
			.path(reqString)
			.build();
	}

	private Response getResponse(URI uri, byte[] requestBody) {
		HttpRequest.Builder reqbuilder = HttpRequest.newBuilder()
			.uri(uri)
			.method(httpServletRequest.getMethod(),
				HttpRequest.BodyPublishers.ofByteArray(requestBody));

		httpServletRequest.getHeaderNames().asIterator()
			.forEachRemaining(key -> {
				// some headers are restricted and cannot be set on the request
				// lua adds ':' to some headers which we filter as they are invalid
				// and not needed for our requests.
				if (Utils.ALLOWED_HEADERS.test(key) && !key.startsWith(":")) {
					reqbuilder.setHeader(key, httpServletRequest.getHeader(key));
				}
			});

		//Add authtoken
		CommonConfig.getInstance().authToken.ifPresent(
			token -> reqbuilder.setHeader(io.cube.agent.Constants.AUTHORIZATION_HEADER, token));

		HttpResponse<byte[]> response = null;
		try {
			response = httpClient
				.send(reqbuilder.build(), BodyHandlers.ofByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return response != null ? createResponse(response) : notFound();
	}

	private Response createResponse(HttpResponse<byte[]> resp) {
		ResponseBuilder builder = Response.status(resp.statusCode());
		MultivaluedMap<String, String> trailersMultiValuedMap = new MultivaluedHashMap<>();
		resp.headers().map().forEach((headerName, headerValList) -> headerValList.forEach((val) -> {
			if (Utils.ALLOWED_HEADERS.test(headerName) && !headerName.startsWith(":") && !headerName
				.startsWith(Constants.MD_TRAILER_HEADER_PREFIX)) {
				builder.header(headerName, val);
			} else if (headerName
				.startsWith(Constants.MD_TRAILER_HEADER_PREFIX)) {
				String realTrailerKey = headerName
					.substring(Constants.MD_TRAILER_HEADER_PREFIX.length());
				trailersMultiValuedMap.add(realTrailerKey, val);
			}
		}));
		addTrailersForGRPC(trailersMultiValuedMap);

		return builder.entity(resp.body()).build();
	}

	private Response notFound() {
		return Response.status(Response.Status.NOT_FOUND).entity("Response not found").build();
	}

	private void addTrailersForGRPC(MultivaluedMap<String, String> trailersMultiValuedMap) {
		//Add trailers for GRPC handling
		if (httpServletResponse != null) {
			// It's necessary to set "Trailer" header when setting trailers
			// https://javaee.github.io/tutorial/servlets014b.html
			httpServletResponse.setTrailerFields(() -> {
				Map<String, String> trailersMap = new HashMap<>();
				for (String key : trailersMultiValuedMap.keySet()) {
					trailersMap.put(key, trailersMultiValuedMap.getFirst(key));
				}
				return trailersMap;
			});
		} else {
			LOGGER.error(
				"httpServletResponse is not injected using context annotation. grpc trailers not set/");
		}
	}

	private HttpClient httpClient;

	@Inject
	public ProxyWS() {

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
	}
}
