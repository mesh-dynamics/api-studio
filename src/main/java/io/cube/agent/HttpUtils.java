package io.cube.agent;

import java.io.IOException;
import java.util.Optional;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

	private static CloseableHttpClient client = HttpClientBuilder.create().build();

	public static Optional<CloseableHttpResponse> getResponse(HttpRequestBase request) {
		int maxNumberOfAttempts = 3; //default

		int numberOfAttempts = 0;
		CloseableHttpResponse response = null;
		while (numberOfAttempts < maxNumberOfAttempts) {
			try {
				LOGGER.info("Sending request " + request.toString());
				response = client.execute(request);
				int responseCode = response.getStatusLine().getStatusCode();
				if (Response.Status.Family.familyOf(responseCode)
					.equals(Response.Status.Family.SUCCESSFUL) || Response.Status.Family
					.familyOf(responseCode).equals(Family.REDIRECTION)) {
					return Optional.of(response);
				}
				numberOfAttempts++;
			} catch (Exception e) {
				LOGGER.error("Error while sending request to cube service", e);
				numberOfAttempts++;
			}
		}
		return Optional.empty();
	}

}
