package io.cube.agent;

import java.util.Optional;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import io.cube.agent.logger.CubeLoggerFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;

public class HttpUtils {

	private static final Logger LOGGER = CubeLoggerFactory.getLogger(HttpUtils.class);

	private static CloseableHttpClient client = HttpClientBuilder.create().build();

	private static final int defaultRetryCount = 2;

	public static Optional<CloseableHttpResponse> getResponse(HttpRequestBase request, Optional<Integer> retryCount) {
		int maxNumberOfAttempts = retryCount.orElse(defaultRetryCount); //default

		int numberOfAttempts = 0;
		CloseableHttpResponse response = null;
		while (numberOfAttempts < maxNumberOfAttempts) {
			try {
				LOGGER.info("Sending request " + request.toString());
				response = client.execute(request);
				int responseCode = response.getStatusLine().getStatusCode();
				if (Response.Status.Family.familyOf(responseCode).equals(Response.Status.Family.SUCCESSFUL) ||
						Response.Status.Family.familyOf(responseCode).equals(Family.REDIRECTION) ||
						Response.Status.Family.familyOf(responseCode).equals(Family.CLIENT_ERROR)) {
					return Optional.of(response);
				} else {
					response.close();
				}
				numberOfAttempts++;
			} catch (Exception e) {
				//This exception stack floods the logs, the stack trace does not provide any
				//additional information, hence only printing the getMessage
				LOGGER.error("Error while sending request to cube service : " + e.getMessage());
				numberOfAttempts++;
			}
		}
		return Optional.empty();
	}

}
