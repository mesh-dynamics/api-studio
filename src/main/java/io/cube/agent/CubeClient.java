package io.cube.agent;

import static io.cube.agent.Utils.ofFormData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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


	public CubeClient(ObjectMapper jsonMapper) throws Exception {
		this.jsonMapper = jsonMapper;
	}

	private Optional<String> getResponse(HttpRequest httpRequest) {
		HttpClient client = CommonConfig.getInstance().getHttpClient();
		CommonConfig config = null;
		try {
			config = CommonConfig.getInstance();
		} catch (Exception e) {
			LOGGER.error("Error while getting Common config instance" , e);
		}
		int maxNumberOfAttempts = config.RETRIES;
		int numberOfAttempts = 0;
		while (numberOfAttempts < maxNumberOfAttempts) {
			try {
				HttpResponse<String> response =
					client.send(httpRequest, BodyHandlers.ofString());
				if (response.statusCode() == 200) {
					return Optional.of(response.body());
				}
				numberOfAttempts++;
			} catch (Exception e) {
				LOGGER.error("Error while sending request to cube service" , e);
				numberOfAttempts++;
			}
		}
		return Optional.empty();
	}

	private Optional<String> getResponse(HttpRequest.Builder requestBuilder, Object reqBody,
		String contentType) {
		try {
			String requestBody = jsonMapper.writeValueAsString(reqBody);
			CommonUtils.addTraceHeaders(requestBuilder, "POST");
			requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.header("Content-Type", contentType);
			return getResponse(requestBuilder.build());
		} catch (JsonProcessingException ex) {
			LOGGER.error("Error while serializing request body", ex);
		}
		return Optional.empty();
	}

	//TODO: Cleanup - phase this out
	public Optional<String> storeFunctionReqResp(FnReqResponse fnReqResponse) {
		HttpRequest recordService = CommonConfig.getInstance().getCubeRecordService();
		URI finalURI = recordService.uri().resolve("cs/").resolve("fr");
		HttpRequest.Builder recordReqbuilder = HttpRequest.newBuilder(finalURI);
		return getResponse(recordReqbuilder, fnReqResponse, TEXT_PLAIN);
	}

	//TODO: Cleanup - phase this out
	public Optional<String> storeSingleReqResp(ReqResp reqResp) {
		HttpRequest recordService = CommonConfig.getInstance().getCubeRecordService();
		URI finalURI = recordService.uri().resolve("cs/").resolve("rr");
		HttpRequest.Builder recordReqbuilder = HttpRequest.newBuilder(finalURI);
		return getResponse(recordReqbuilder, reqResp, TEXT_PLAIN);
	}


	//TODO: Cleanup - phase this out
	public Optional<FnResponse> getMockResponse(FnReqResponse fnReqResponse) {
		HttpRequest mockService = CommonConfig.getInstance().getCubeMockService();
		URI finalURI = mockService.uri().resolve("ms/").resolve("fr");
		HttpRequest.Builder mockReqbuilder = HttpRequest.newBuilder(finalURI);
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
		HttpRequest mockService = CommonConfig.getInstance().getCubeMockService();
		URI finalURI = mockService.uri().resolve("ms/").resolve("mockFunction");
		HttpRequest.Builder mockReqbuilder = HttpRequest.newBuilder(finalURI);

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
		HttpRequest mockService = CommonConfig.getInstance().getCubeMockService();
		URI finalURI = mockService.uri().resolve("ms/").resolve("thrift");
		HttpRequest.Builder mockReqbuilder = HttpRequest.newBuilder(finalURI);
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
		HttpRequest recordService = CommonConfig.getInstance().getCubeRecordService();
		URI finalURI = recordService.uri().resolve("cs/").resolve("start/")
			.resolve(customerid.concat("/")).resolve(app.concat("/"))
			.resolve(instanceid.concat("/")).resolve(collection);

		HttpRequest.Builder recordReqbuilder = HttpRequest.newBuilder(finalURI)
			.POST(ofFormData(new HashMap<>()))
			.header("Content-Type", APPLICATION_FORM_URL_ENCODED);
		CommonUtils.addTraceHeaders(recordReqbuilder, "POST");

		return getResponse(recordReqbuilder.build());
	}

	public Optional<String> stopRecording(String customerid, String app, String collection) {
		HttpRequest recordService = CommonConfig.getInstance().getCubeRecordService();
		URI finalURI = recordService.uri().resolve("cs/").resolve("stop/")
			.resolve(customerid.concat("/")).resolve(app.concat("/"))
			.resolve(collection);

		HttpRequest.Builder recordReqbuilder = HttpRequest.newBuilder(finalURI)
			.POST(ofFormData(new HashMap<>()))
			.header("Content-Type", APPLICATION_FORM_URL_ENCODED);
		CommonUtils.addTraceHeaders(recordReqbuilder, "POST");

		return getResponse(recordReqbuilder.build());
	}

	public Optional<String> initReplay(String customerid, String app, String instanceid,
		String collection, String endpoint) {
		HttpRequest recordService = CommonConfig.getInstance().getCubeRecordService();
		URI finalURI = recordService.uri().resolve("rs/").resolve("init/")
			.resolve(customerid.concat("/")).resolve(app.concat("/"))
			.resolve(collection);

		Map<Object, Object> params = new HashMap<>();
		params.put("instanceid", instanceid);
		params.put("endpoint", endpoint);

		HttpRequest.Builder recordReqbuilder = HttpRequest.newBuilder(finalURI)
			.POST(ofFormData(params))
			.header("Content-Type", APPLICATION_FORM_URL_ENCODED);
		CommonUtils.addTraceHeaders(recordReqbuilder, "POST");

		return getResponse(recordReqbuilder.build());
	}

	public Optional<String> forceStartReplay(String replayid) {
		HttpRequest recordService = CommonConfig.getInstance().getCubeRecordService();
		URI finalURI = recordService.uri().resolve("rs/").resolve("forcestart/")
			.resolve(replayid);

		HttpRequest.Builder recordReqbuilder = HttpRequest.newBuilder(finalURI)
			.POST(ofFormData(new HashMap<>()))
			.header("Content-Type", APPLICATION_FORM_URL_ENCODED);
		CommonUtils.addTraceHeaders(recordReqbuilder, "POST");

		return getResponse(recordReqbuilder.build());
	}


	public Optional<String> forceCompleteReplay(String replayid) {
		HttpRequest recordService = CommonConfig.getInstance().getCubeRecordService();
		URI finalURI = recordService.uri().resolve("rs/").resolve("forcecomplete/")
			.resolve(replayid);

		HttpRequest.Builder recordReqbuilder = HttpRequest.newBuilder(finalURI)
			.POST(ofFormData(new HashMap<>()))
			.header("Content-Type", APPLICATION_FORM_URL_ENCODED);
		CommonUtils.addTraceHeaders(recordReqbuilder, "POST");

		return getResponse(recordReqbuilder.build());
	}

	public Optional<String> storeEvent(Event event) {
		HttpRequest recordService = CommonConfig.getInstance().getCubeRecordService();
		URI finalURI = recordService.uri().resolve("cs/").resolve("storeEvent");
		HttpRequest.Builder recordReqbuilder = HttpRequest.newBuilder(finalURI)
			.header("Content-Type", "application/json");

		try {
			String requestBody = jsonMapper.writeValueAsString(event);
			LOGGER.debug("event : ".concat(requestBody));
			CommonUtils.addTraceHeaders(recordReqbuilder, "POST");
			recordReqbuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
			return getResponse(recordReqbuilder.build());
		} catch (JsonProcessingException e) {
			LOGGER.error("Store event result in exception", e);
		}
		return Optional.empty();
	}

}