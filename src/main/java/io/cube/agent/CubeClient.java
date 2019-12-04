package io.cube.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;

/**
 * Client to connect to cube service
 */
public class CubeClient {

    private Client restClient = null;
    private WebTarget cubeRecordService = null;
    private WebTarget cubeMockService = null;
    private final int maxNumberOfAttempts;
    private ObjectMapper jsonMapper;

    private static final Logger LOGGER = LogManager.getLogger(CubeClient.class);

    public CubeClient(ObjectMapper jsonMapper) {
        CommonConfig config = new CommonConfig();
        ClientConfig clientConfig = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, config.READ_TIMEOUT)
                .property(ClientProperties.CONNECT_TIMEOUT, config.CONNECT_TIMEOUT);
        restClient = ClientBuilder.newClient(clientConfig);
        cubeRecordService = restClient.target(config.CUBE_RECORD_SERVICE_URI);
        cubeMockService = restClient.target(config.CUBE_MOCK_SERVICE_URI);
        maxNumberOfAttempts = config.RETRIES;
        this.jsonMapper = jsonMapper;
    }


    private Optional<String> getResponse(Invocation invocation) {
        int numberOfAttempts = 0;
        while (numberOfAttempts < maxNumberOfAttempts) {
            try {
                Response response = invocation.invoke();
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    return Optional.of(response.readEntity(String.class));
                }
                numberOfAttempts++;
            } catch (Exception e) {
                LOGGER.error("Error while sending request to cube service :: " + e.getMessage());
                numberOfAttempts++;
            }
        }
        return Optional.empty();
    }


    private Optional<String> getResponse(Invocation.Builder builder, FnReqResponse fnReqResponse) {
        try {
            String jsonEntity = jsonMapper.writeValueAsString(fnReqResponse);
            CommonUtils.addTraceHeaders(builder , "POST");
            return getResponse(builder.buildPost(Entity.entity(jsonEntity, MediaType.TEXT_PLAIN)));
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while serializing function req/resp object :: "
                    + e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<String> getResponse(Invocation.Builder builder, ReqResp reqResp) {
        try {
            String jsonEntity = jsonMapper.writeValueAsString(reqResp);
            CommonUtils.addTraceHeaders(builder , "POST");
            return getResponse(builder.buildPost(Entity.entity(jsonEntity, MediaType.TEXT_PLAIN)));
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while serializing single HTTP req/resp object :: "
                + e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<String> getResponse(Invocation.Builder builder, Event event) {
        try {
            String jsonEntity = jsonMapper.writeValueAsString(event);
            CommonUtils.addTraceHeaders(builder, "POST");
            return getResponse(builder.buildPost(Entity.entity(jsonEntity, MediaType.APPLICATION_JSON)));
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while serializing function req/resp object :: "
                    + e.getMessage());
        }
        return Optional.empty();
    }


    public Optional<String> storeFunctionReqResp(FnReqResponse fnReqResponse) {
        Invocation.Builder builder = cubeRecordService.path("cs").path("fr").request(MediaType.TEXT_PLAIN);
        return getResponse(builder , fnReqResponse);
    }

    public Optional<String> storeSingleReqResp(ReqResp reqResp) {
        Invocation.Builder builder = cubeRecordService.path("cs").path("rr").request(MediaType.TEXT_PLAIN);
        return getResponse(builder , reqResp);
    }


    public Optional<FnResponse> getMockResponse(FnReqResponse fnReqResponse) {
        Invocation.Builder builder = cubeMockService.path("ms").path("fr").request(MediaType.TEXT_PLAIN);
        return getResponse(builder, fnReqResponse).flatMap(response -> {
            try {
                LOGGER.debug("GOT RESPONSE :: " + response);
                return Optional.of(jsonMapper.readValue(response, FnResponse.class));
            } catch (Exception e) {
                LOGGER.error("Error while parsing json response from mock server :: " + e.getMessage());
                return Optional.empty();
            }
        });
    }

    public Optional<FnResponse> getMockResponse(Event event) {
        Invocation.Builder builder = cubeMockService.path("ms").path("mockFunction").request(MediaType.TEXT_PLAIN);
        return getResponse(builder, event).flatMap(response -> {
            try {
                LOGGER.debug("GOT RESPONSE :: " + response);
                return Optional.of(jsonMapper.readValue(response, FnResponse.class));
            } catch (Exception e) {
                LOGGER.error("Error while parsing json response from mock server :: " + e.getMessage());
                return Optional.empty();
            }
        });
    }

    public Optional<String> startRecording(String customerid, String app, String instanceid, String collection) {
        Invocation.Builder builder =
                cubeRecordService.path("cs").path("start").path(customerid).path(app).path(instanceid).path(collection)
                        .request(MediaType.APPLICATION_FORM_URLENCODED);
        CommonUtils.addTraceHeaders(builder , "POST");
        return getResponse(builder.buildPost(Entity.form(new MultivaluedHashMap<>())));
    }

    public Optional<String> stopRecording(String customerid, String app, String collection) {
        Invocation.Builder builder =
                cubeRecordService.path("cs").path("stop").path(customerid).path(app).path(collection)
                        .request(MediaType.APPLICATION_FORM_URLENCODED);
        CommonUtils.addTraceHeaders(builder , "POST");
        return getResponse(builder.buildPost(Entity.form(new MultivaluedHashMap<>())));
    }

    public Optional<String> initReplay(String customerid, String app, String instanceid, String collection,
                                       String endpoint) {
        Invocation.Builder builder =
                cubeRecordService.path("rs").path("init").path(customerid).path(app).path(collection)
                        .request(MediaType.APPLICATION_FORM_URLENCODED);
        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.add("instanceid", instanceid);
        params.add("endpoint", endpoint);
        CommonUtils.addTraceHeaders(builder , "POST");
        return getResponse(builder.buildPost(Entity.form(params)));
    }

    public Optional<String> forceStartReplay(String replayid) {
        Invocation.Builder builder =
                cubeRecordService.path("rs").path("forcestart").path(replayid).request();
        CommonUtils.addTraceHeaders(builder , "POST");
        return getResponse(builder.buildPost(Entity.form(new MultivaluedHashMap<>())));
    }


    public Optional<String> forceCompleteReplay(String replayid) {
        Invocation.Builder builder =
                cubeRecordService.path("rs").path("forcecomplete").path(replayid).request();
        CommonUtils.addTraceHeaders(builder , "POST");
        return getResponse(builder.buildPost(Entity.form(new MultivaluedHashMap<>())));
    }

    public Optional<String> storeEvent(Event event) {
        Invocation.Builder builder = cubeRecordService.path("cs").path("storeEvent").request(MediaType.TEXT_PLAIN);
        try {
            String jsonEntity = jsonMapper.writeValueAsString(event);
            LOGGER.debug(new ObjectMessage(Map.of("event", jsonEntity)));
            CommonUtils.addTraceHeaders(builder , "POST");
            return getResponse(builder.buildPost(Entity.entity(jsonEntity, MediaType.APPLICATION_JSON)));
        } catch (JsonProcessingException e) {
            LOGGER.error(new ObjectMessage(Map.of("operation",
                    "Store Event", "response" , "Json Exception")) , e);
        }
        return Optional.empty();
    }
}