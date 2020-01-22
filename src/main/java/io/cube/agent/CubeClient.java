package io.cube.agent;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

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


	private static final Logger LOGGER = LogManager.getLogger(CubeClient.class);

	WebTarget getCubeRecordService() {
		CommonConfig config = null;
		try {
			config = CommonConfig.getInstance();
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Error while getting Common config instance" )), e);
		}
		ClientConfig clientConfig = new ClientConfig()
			.property(ClientProperties.READ_TIMEOUT, config.READ_TIMEOUT)
			.property(ClientProperties.CONNECT_TIMEOUT, config.CONNECT_TIMEOUT);
		Client restClient = ClientBuilder.newClient(clientConfig);
		WebTarget cubeRecordService = restClient.target(config.CUBE_RECORD_SERVICE_URI);
		return cubeRecordService;
	}

	WebTarget getCubeMockService()  {
		CommonConfig config = null;
		try {
			config = CommonConfig.getInstance();
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Error while getting Common config instance" )), e);
		}
		ClientConfig clientConfig = new ClientConfig()
			.property(ClientProperties.READ_TIMEOUT, config.READ_TIMEOUT)
			.property(ClientProperties.CONNECT_TIMEOUT, config.CONNECT_TIMEOUT);
		Client restClient = ClientBuilder.newClient(clientConfig);
		WebTarget cubeMockService = restClient.target(config.CUBE_MOCK_SERVICE_URI);
		return cubeMockService;
	}

	public CubeClient(ObjectMapper jsonMapper) throws Exception {
		this.jsonMapper = jsonMapper;
	}


	private Optional<String> getResponse(Invocation invocation) {
		CommonConfig config = null;
		try {
			config = CommonConfig.getInstance();
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Error while getting Common config instance" )), e);
		}
		int maxNumberOfAttempts = config.RETRIES;
		int numberOfAttempts = 0;
		while (numberOfAttempts < maxNumberOfAttempts) {
			try {
				Response response = invocation.invoke();
				if (response.getStatus() == Response.Status.OK.getStatusCode()) {
					return Optional.of(response.readEntity(String.class));
				}
				numberOfAttempts++;
			} catch (Exception e) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
					"Error while sending request to cube service" )), e);
				numberOfAttempts++;
			}
		}
		return Optional.empty();
	}


	private Optional<String> getResponse(Invocation.Builder builder, FnReqResponse fnReqResponse) {
		try {
			String jsonEntity = jsonMapper.writeValueAsString(fnReqResponse);
			CommonUtils.addTraceHeaders(builder, "POST");
			return getResponse(builder.buildPost(Entity.entity(jsonEntity, MediaType.TEXT_PLAIN)));
		} catch (JsonProcessingException e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Error while serializing function req/resp object" )), e);
		}
		return Optional.empty();
	}

	private Optional<String> getResponse(Invocation.Builder builder, ReqResp reqResp) {
		try {
			String jsonEntity = jsonMapper.writeValueAsString(reqResp);
			CommonUtils.addTraceHeaders(builder, "POST");
			return getResponse(builder.buildPost(Entity.entity(jsonEntity, MediaType.TEXT_PLAIN)));
		} catch (JsonProcessingException e) {
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE,
					"Error while serializing single HTTP req/resp object")),e);
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE,
					"Error while getting response")),e);
		}
		return Optional.empty();
	}

	private Optional<String> getResponse(Invocation.Builder builder, Event event)  {
		try {
			String jsonEntity = jsonMapper.writeValueAsString(event);
			CommonUtils.addTraceHeaders(builder, "POST");
			return getResponse(
				builder.buildPost(Entity.entity(jsonEntity, MediaType.APPLICATION_JSON)));
		} catch (JsonProcessingException e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE ,
				"Error while serializing event")), e);
		}
		return Optional.empty();
	}


	public Optional<String> storeFunctionReqResp(FnReqResponse fnReqResponse)  {
		WebTarget cubeRecordService = getCubeRecordService();
		Invocation.Builder builder = cubeRecordService.path("cs").path("fr")
			.request(MediaType.TEXT_PLAIN);
		return getResponse(builder, fnReqResponse);
	}

	public Optional<String> storeSingleReqResp(ReqResp reqResp)  {
		WebTarget cubeRecordService = getCubeRecordService();
		Invocation.Builder builder = cubeRecordService.path("cs").path("rr")
			.request(MediaType.TEXT_PLAIN);
		return getResponse(builder, reqResp);
	}


	public Optional<FnResponse> getMockResponse(FnReqResponse fnReqResponse)  {
		WebTarget cubeMockService = getCubeMockService();
		Invocation.Builder builder = cubeMockService.path("ms").path("fr")
			.request(MediaType.TEXT_PLAIN);
		return getResponse(builder, fnReqResponse).flatMap(response -> {
			try {
				LOGGER.debug(new ObjectMessage(Map.of("Response" , response)));
				return Optional.of(jsonMapper.readValue(response, FnResponse.class));
			} catch (Exception e) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE ,
					"Error while parsing json response from mock server")), e);
				return Optional.empty();
			}
		});
	}

	public Optional<FnResponse> getMockResponse(Event event) {
		WebTarget cubeMockService = getCubeMockService();
		Invocation.Builder builder = cubeMockService.path("ms").path("mockFunction")
			.request(MediaType.TEXT_PLAIN);
		return getResponse(builder, event).flatMap(response -> {
			try {
				LOGGER.debug(new ObjectMessage(Map.of("Response" , response)));
				return Optional.of(jsonMapper.readValue(response, FnResponse.class));
			} catch (Exception e) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE ,
					"Error while parsing json response from mock server")), e);
				return Optional.empty();
			}
		});
	}

	public Optional<Event> getMockThriftResponse(Event event)  {
		WebTarget cubeMockService = getCubeMockService();
		Invocation.Builder builder = cubeMockService.path("ms").path("thrift").request(MediaType.APPLICATION_JSON);
		return getResponse(builder, event).flatMap(response -> {
			try {
				LOGGER.debug(new ObjectMessage(Map.of("Response" , response)));
				return Optional.of(jsonMapper.readValue(response, Event.class));
			} catch (Exception e) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE ,
					"Error while parsing json response from mock server")), e);
				return Optional.empty();
			}
		});
	}

	public Optional<String> startRecording(String customerid, String app, String instanceid,
		String collection) {
		WebTarget cubeRecordService = getCubeRecordService();
		Invocation.Builder builder =
			cubeRecordService.path("cs").path("start").path(customerid).path(app).path(instanceid)
				.path(collection)
				.request(MediaType.APPLICATION_FORM_URLENCODED);
		CommonUtils.addTraceHeaders(builder, "POST");
		return getResponse(builder.buildPost(Entity.form(new MultivaluedHashMap<>())));
	}

	public Optional<String> stopRecording(String customerid, String app, String collection) {
		WebTarget cubeRecordService = getCubeRecordService();
		Invocation.Builder builder =
			cubeRecordService.path("cs").path("stop").path(customerid).path(app).path(collection)
				.request(MediaType.APPLICATION_FORM_URLENCODED);
		CommonUtils.addTraceHeaders(builder, "POST");
		return getResponse(builder.buildPost(Entity.form(new MultivaluedHashMap<>())));
	}

	public Optional<String> initReplay(String customerid, String app, String instanceid,
		String collection,
		String endpoint) {
		WebTarget cubeRecordService = getCubeRecordService();
		Invocation.Builder builder =
			cubeRecordService.path("rs").path("init").path(customerid).path(app).path(collection)
				.request(MediaType.APPLICATION_FORM_URLENCODED);
		MultivaluedMap<String, String> params = new MultivaluedHashMap();
		params.add("instanceid", instanceid);
		params.add("endpoint", endpoint);
		CommonUtils.addTraceHeaders(builder, "POST");
		return getResponse(builder.buildPost(Entity.form(params)));
	}

	public Optional<String> forceStartReplay(String replayid)  {
		WebTarget cubeRecordService = getCubeRecordService();
		Invocation.Builder builder =
			cubeRecordService.path("rs").path("forcestart").path(replayid).request();
		CommonUtils.addTraceHeaders(builder, "POST");
		return getResponse(builder.buildPost(Entity.form(new MultivaluedHashMap<>())));
	}


	public Optional<String> forceCompleteReplay(String replayid) {
		WebTarget cubeRecordService = getCubeRecordService();
		Invocation.Builder builder =
			cubeRecordService.path("rs").path("forcecomplete").path(replayid).request();
		CommonUtils.addTraceHeaders(builder, "POST");
		return getResponse(builder.buildPost(Entity.form(new MultivaluedHashMap<>())));
	}

	public Optional<String> storeEvent(Event event) {
		WebTarget cubeRecordService = getCubeRecordService();
		Invocation.Builder builder = cubeRecordService.path("cs").path("storeEvent")
			.request(MediaType.TEXT_PLAIN);
		try {
			String jsonEntity = jsonMapper.writeValueAsString(event);
			LOGGER.debug(new ObjectMessage(Map.of("event", jsonEntity)));
			CommonUtils.addTraceHeaders(builder, "POST");
			return getResponse(
				builder.buildPost(Entity.entity(jsonEntity, MediaType.APPLICATION_JSON)));
		} catch (JsonProcessingException e) {
			LOGGER.error(new ObjectMessage(Map.of("operation",
				"Store Event", "response", "Json Exception")), e);
		}
		return Optional.empty();
	}

}