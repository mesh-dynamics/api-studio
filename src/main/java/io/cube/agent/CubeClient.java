package io.cube.agent;

import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        Config config = new Config();
        ClientConfig clientConfig = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, config.READ_TIMEOUT)
                .property(ClientProperties.CONNECT_TIMEOUT, config.CONNECT_TIMEOUT);
        restClient = ClientBuilder.newClient(clientConfig);
        cubeRecordService = restClient.target(config.CUBE_RECORD_SERVICE_URI);
        cubeMockService = restClient.target(config.CUBE_MOCK_SERVICE_URI);
        maxNumberOfAttempts = config.RETRIES;
        this.jsonMapper = jsonMapper;
    }

    private Optional<String> getResponse(Invocation.Builder builder, FnReqResponse fnReqResponse) {
        try {
        String jsonEntity = jsonMapper.writeValueAsString(fnReqResponse);
        int numberOfAttempts = 0;
        while (numberOfAttempts < maxNumberOfAttempts) {
            try {
                Response response = builder.post(Entity.entity(jsonEntity, MediaType.TEXT_PLAIN));
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    return Optional.of(response.readEntity(String.class));
                }
                numberOfAttempts++;
            } catch (Exception e) {
                LOGGER.error("Error while sending request to cube service :: " + e.getMessage());
                numberOfAttempts++;
            }
        }} catch (JsonProcessingException e) {
            LOGGER.error("Error while serializing function req/resp object :: "
                    + e.getMessage());
        }
        return Optional.empty();
    }


    public Optional<String> storeFunctionReqResp(FnReqResponse fnReqResponse) {
        Invocation.Builder builder = cubeRecordService.path("cs").path("fr").request(MediaType.TEXT_PLAIN);
        return getResponse(builder , fnReqResponse);
    }

    public Optional<String> getMockResponse(FnReqResponse fnReqResponse) {
        Invocation.Builder builder = cubeMockService.path("ms").path("fr").request(MediaType.TEXT_PLAIN);
        return getResponse(builder, fnReqResponse);
    }

}