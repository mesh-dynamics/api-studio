package io.md.drivers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.async.MinimalHttpAsyncClient;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.AsyncClientEndpoint;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.config.H2Config;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.md.drivers.GrpcReplayDriver.Http2Client.DummyCB;

public class MDHttp2Client {

	private static Logger LOGGER = LogManager.getLogger(MDHttp2Client.class);

	// Full URI with path and query params
	private final URI uri;

	private final String method;

	private final byte[] body;

	private final MultivaluedMap<String , String> headers;

	public MDHttp2Client(URI uri , String method, byte[] body , MultivaluedMap<String , String> headers ){
		this.uri = uri;
		this.method = method.toUpperCase();
		this.body = body;
		this.headers = headers!=null ? headers : new MultivaluedHashMap<>();
	}

	public SimpleHttpResponse makeRequest()
		throws InterruptedException, ExecutionException, TimeoutException, URISyntaxException {

		SimpleHttpRequest simpleHttpRequest = new SimpleHttpRequest(method , uri);
		if(body!=null) simpleHttpRequest.setBody(body , null);

		headers.forEach((k, vlist) -> {
				vlist.forEach(value -> simpleHttpRequest.setHeader(k, value));
		});

		return getSimpleHttpResponse(uri , simpleHttpRequest);
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
