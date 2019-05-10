package io.cube.agent;

import java.awt.*;
import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CubeClient {

    private Client restClient = null;
    private WebTarget cubeRecordService = null;
    private WebTarget cubeMockService = null;
    private int maxNumberOfAttempts = 3;
    private ObjectMapper jsonMapper;

    private static final Logger LOGGER = LogManager.getLogger(CubeClient.class);

    private static String CUBE_RECORD_SERVICE_URI = "http://cubews:9080";
    private static String CUBE_MOCK_SERVICE_URI = "http://cubews:9080";

    public CubeClient(ObjectMapper jsonMapper) {
        ClientConfig clientConfig = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, 100000)
                .property(ClientProperties.CONNECT_TIMEOUT, 10000);
        restClient = ClientBuilder.newClient(clientConfig);
        cubeRecordService = restClient.target(CUBE_RECORD_SERVICE_URI);
        cubeMockService = restClient.target(CUBE_MOCK_SERVICE_URI);
        this.jsonMapper = jsonMapper;
    }

    public Optional<String> storeFunctionReqResp(FnReqResponse fnReqResponse) {
        try {
            String jsonEntity = jsonMapper.writeValueAsString(fnReqResponse);

            int numberOfAttempts = 0;
            while (numberOfAttempts < maxNumberOfAttempts) {
                try {
                    Response response = cubeRecordService.path("cs").path("fr").request(MediaType.APPLICATION_JSON).
                            post(Entity.entity(jsonEntity, MediaType.APPLICATION_JSON));
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        return Optional.of(response.getEntity().toString());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error while sending request to cube service :: " + e.getMessage());
                    numberOfAttempts++;
                }
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while serializing function req/resp object :: "
                    + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<String> getMockResponse(FnReqResponse fnReqResponse) {
        int numberOfAttempts = 0;
        while(numberOfAttempts < maxNumberOfAttempts) {
            try {

            } catch (Exception e) {
                LOGGER.error("Error while ");
            }
        }



        return Optional.empty();
    }

}