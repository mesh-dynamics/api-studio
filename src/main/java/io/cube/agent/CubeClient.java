package io.cube.agent;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.http.Consts;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.constants.Constants;
import io.md.dao.Event;
import io.md.utils.CommonUtils;

/**
 * Client to connect to cube service
 */
public class CubeClient {

	private ObjectMapper jsonMapper;


	private static final String TEXT_PLAIN = "text/plain";
	private static final String APPLICATION_JSON = "application/json";
	private static final String APPLICATION_FORM_URL_ENCODED = "application/x-www-form-urlencoded";
	private static final Logger LOGGER = LoggerFactory.getLogger(CubeClient.class);


	public CubeClient(ObjectMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	private Optional<String> getResponse(HttpPost postRequest) {
		CloseableHttpClient client = CommonConfig.getInstance().getHttpClient();
		CommonConfig config = null;
		try {
			config = CommonConfig.getInstance();
		} catch (Exception e) {
			LOGGER.error("Error while getting Common config instance", e);
		}
		int maxNumberOfAttempts = config.RETRIES;
		int numberOfAttempts = 0;
		CloseableHttpResponse response = null;
		while (numberOfAttempts < maxNumberOfAttempts) {
			try {
				response = client.execute(postRequest);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String responseString = new BasicResponseHandler().handleResponse(response);
					return Optional.ofNullable(responseString);
				}
				numberOfAttempts++;
			} catch (Exception e) {
				LOGGER.error("Error while sending request to cube service", e);
				numberOfAttempts++;
			} finally {
				try {
					if (response != null) {
						response.close();
					}
				} catch (IOException e) {
					LOGGER.error("Error while closing the connection", e);
				}
			}
		}
		return Optional.empty();
	}

	private Optional<String> getResponse(HttpPost requestBuilder, Object reqBody,
		String contentType) {
		try {
			String requestBody = jsonMapper.writeValueAsString(reqBody);
			CommonUtils.addTraceHeaders(requestBuilder, "POST");
			StringEntity requestEntity = new StringEntity(requestBody, Consts.UTF_8);
			requestEntity.setContentType(contentType);
			requestBuilder.setEntity(requestEntity);
			requestBuilder.setHeader("Content-Type", contentType);
			return getResponse(requestBuilder);
		} catch (JsonProcessingException ex) {
			LOGGER.error("Error while serializing request body", ex);
		} catch (UnsupportedCharsetException ex2) {
			LOGGER.error("Invalid string entity", ex2);
		}
		return Optional.empty();
	}

	//TODO: Cleanup - phase this out
	public Optional<String> storeFunctionReqResp(FnReqResponse fnReqResponse) {
		URI recordURI = URI.create(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
			.resolve("cs/").resolve("fr");

		HttpPost recordReqbuilder = new HttpPost(recordURI);
		return getResponse(recordReqbuilder, fnReqResponse, TEXT_PLAIN);
	}

	//TODO: Cleanup - phase this out
	public Optional<String> storeSingleReqResp(ReqResp reqResp) {
		URI recordURI = URI.create(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
			.resolve("cs/").resolve("rr");
		HttpPost recordReqbuilder = new HttpPost(recordURI);
		return getResponse(recordReqbuilder, reqResp, TEXT_PLAIN);
	}


	//TODO: Cleanup - phase this out
	public Optional<FnResponse> getMockResponse(FnReqResponse fnReqResponse) {
		URI mockURI = URI.create(CommonConfig.getInstance().CUBE_MOCK_SERVICE_URI)
			.resolve("ms/").resolve("fr");
		HttpPost mockReqbuilder = new HttpPost(mockURI);
		return getResponse(mockReqbuilder, fnReqResponse, TEXT_PLAIN).flatMap(response -> {
			try {
				LOGGER.debug("Response : ".concat(response));
				return Optional.of(jsonMapper.readValue(response, FnResponse.class));
			} catch (Exception e) {
				LOGGER.error("Error while parsing json response from mock server", e);
				return Optional.empty();
			}
		});
	}

	public Optional<FnResponse> getMockResponse(Event event) {
		URI mockURI = URI.create(CommonConfig.getInstance().CUBE_MOCK_SERVICE_URI)
			.resolve("ms/").resolve("mockFunction");
		HttpPost mockReqbuilder = new HttpPost(mockURI);
		return getResponse(mockReqbuilder, event, APPLICATION_JSON).flatMap(response -> {
			try {
				LOGGER.debug("Response : ".concat(response));
				return Optional.of(jsonMapper.readValue(response, FnResponse.class));
			} catch (Exception e) {
				LOGGER.error("Error while parsing json response from mock server", e);
				return Optional.empty();
			}
		});
	}

	public Optional<Event> getMockThriftResponse(Event event) {
		URI mockURI = URI.create(CommonConfig.getInstance().CUBE_MOCK_SERVICE_URI)
			.resolve("ms/").resolve("thrift");
		HttpPost mockReqbuilder = new HttpPost(mockURI);
		return getResponse(mockReqbuilder, event, APPLICATION_JSON).flatMap(response -> {
			try {
				LOGGER.debug("Response : ".concat(response));
				return Optional.of(jsonMapper.readValue(response, Event.class));
			} catch (Exception e) {
				LOGGER.error("Error while parsing json response from mock server", e);
				return Optional.empty();
			}
		});
	}

	public Optional<String> startRecording(String customerid, String app, String instanceid,
		String collection) {
		URI recordURI = URI.create(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
			.resolve("cs/").resolve("start/")
			.resolve(customerid.concat("/")).resolve(app.concat("/"))
			.resolve(instanceid.concat("/")).resolve(collection);
		return getResponse(recordURI);
	}

	public Optional<String> stopRecording(String customerid, String app, String collection) {
		URI recordURI = URI.create(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
			.resolve("cs/").resolve("stop/")
			.resolve(customerid.concat("/")).resolve(app.concat("/"))
			.resolve(collection);
		return getResponse(recordURI);
	}

	public Optional<String> initReplay(String customerid, String app, String instanceid,
		String collection, String endpoint) {
		URI recordURI = URI.create(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
			.resolve("rs/").resolve("init/")
			.resolve(customerid.concat("/")).resolve(app.concat("/"))
			.resolve(collection);

		HttpPost recordReqbuilder = new HttpPost(recordURI);
		List<NameValuePair> form = new ArrayList<>();
		form.add(new BasicNameValuePair("instanceid", instanceid));
		form.add(new BasicNameValuePair("endpoint", endpoint));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
		recordReqbuilder.setEntity(entity);
		CommonUtils.addTraceHeaders(recordReqbuilder, "POST");
		recordReqbuilder.setHeader("Content-Type", APPLICATION_FORM_URL_ENCODED);

		return getResponse(recordReqbuilder);
	}

	public Optional<String> forceStartReplay(String replayid) {
		URI recordURI = URI.create(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
			.resolve("rs/").resolve("forcestart/")
			.resolve(replayid);
		return getResponse(recordURI);
	}


	public Optional<String> forceCompleteReplay(String replayid) {
		URI recordURI = URI.create(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
			.resolve("rs/").resolve("forcecomplete/")
			.resolve(replayid);
		return getResponse(recordURI);
	}

	public Optional<String> storeEvent(Event event) {
		URI recordURI = URI.create(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
			.resolve("cs/").resolve("storeEvent");
		HttpPost recordReqbuilder = new HttpPost(recordURI);
		recordReqbuilder.setHeader(Constants.CONTENT_TYPE, APPLICATION_JSON);

		try {
			String requestBody = jsonMapper.writeValueAsString(event);
			LOGGER.debug("event : ".concat(requestBody));
			CommonUtils.addTraceHeaders(recordReqbuilder, "POST");
			StringEntity requestEntity = new StringEntity(requestBody, Consts.UTF_8);
			requestEntity.setContentType(APPLICATION_JSON);
			recordReqbuilder.setEntity(requestEntity);
			return getResponse(recordReqbuilder);
		} catch (JsonProcessingException e) {
			LOGGER.error("Store event result in exception", e);
		}
		return Optional.empty();
	}

	private Optional<String> getResponse(URI recordURI) {
		HttpPost recordReqbuilder = new HttpPost(recordURI);
		List<NameValuePair> form = new ArrayList<>();
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
		recordReqbuilder.setEntity(entity);
		recordReqbuilder.setHeader("Content-Type", APPLICATION_FORM_URL_ENCODED);
		CommonUtils.addTraceHeaders(recordReqbuilder, "POST");

		return getResponse(recordReqbuilder);
	}

}