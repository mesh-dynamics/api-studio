package com.cubeui.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import io.md.dao.ApiTraceResponse;
import io.md.dao.Recording;
import io.md.dao.Replay;
import com.cubeui.backend.web.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static com.cubeui.backend.security.Constants.CUBE_SERVER_HREF;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.*;

@Service
@Transactional
@Slf4j
public class CubeServerService {

    @Value("${cube.server.baseUrl.replay}")
    private String cubeServerBaseUrlReplay = CUBE_SERVER_HREF;
    @Value("${cube.server.baseUrl.mock}")
    private String cubeServerBaseUrlMock = CUBE_SERVER_HREF;
    @Value("${cube.server.baseUrl.record}")
    private String cubeServerBaseUrlRecord = CUBE_SERVER_HREF;

    @Autowired
    @Qualifier("appRestClient")
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper jsonMapper;

    @PostConstruct
    protected void init()  {
        //System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        SimpleClientHttpRequestFactory clientHttpRequestFactory
                = new SimpleClientHttpRequestFactory();
        //Connect timeout
        clientHttpRequestFactory.setConnectTimeout(5000);

        //Read timeout
        clientHttpRequestFactory.setReadTimeout(600000);
        restTemplate.setRequestFactory(clientHttpRequestFactory);
    }

    public ResponseEntity fetchGetResponse(String path, String query){
        try {
            URI uri = new URI(null, null, null, 0, path, query, null);
            String result = restTemplate.getForObject(uri, String.class);
            return ok().body(result);
        } catch (URISyntaxException e){
            log.error("Error while retrieving the data from "+ path + " with message="+ e.getMessage());
            return noContent().build();
        } catch (HttpClientErrorException e){
            log.error("Error while retrieving the data from "+ path + " with statusCode=" + e.getStatusCode() + ",message="+ e.getLocalizedMessage());
            return status(e.getStatusCode()).body(new ErrorResponse(e.getLocalizedMessage()));
        } catch (Exception e){
            log.error("Error while retrieving the data from "+ path + " with message="+ e.getMessage());
            return status(NOT_FOUND).body(e);
        }
    }

    public Optional<Replay> getReplay(String replayId) {
        final String path  = cubeServerBaseUrlReplay + "/rs/status/" + replayId;
        final ResponseEntity  response = fetchGetResponse(path, null);
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                final String body = response.getBody().toString();
                final Replay replay = jsonMapper.readValue(body, Replay.class);
                return Optional.of(replay);
            } catch (Exception e) {
                log.info("Error in converting Json to Replay" + replayId + " message"  + e.getMessage());
                return Optional.empty();
            }
        }
        else {
            log.error("Error while retrieving the data from "+ path + " with statusCode="+ response.getStatusCode() +", message="+response.getBody());
            return Optional.empty();
        }
    }

    public Optional<Recording> getRecording(String recordingId) {
        final String path  = cubeServerBaseUrlRecord + "/cs/status/" + recordingId;
        final ResponseEntity  response = fetchGetResponse(path, null);
        return getRecordingFromResponseEntity(response, path);
    }

    public Optional<Recording> searchRecording(String query) {
        String path = cubeServerBaseUrlRecord + "/cs/searchRecording";
        ResponseEntity response = fetchGetResponse(path, query);
        return getRecordingFromResponseEntity(response, path+ "?" +query);
    }

    public Optional<Recording> getRecordingFromResponseEntity(ResponseEntity response, String request) {
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                final String body = response.getBody().toString();
                TypeReference<List<Recording>> mapType = new TypeReference<List<Recording>>() {};
                final List<Recording> recordings = jsonMapper.readValue(body, mapType);
                return recordings.stream().findFirst();
            } catch (Exception e) {
                log.info(String.format("Error in converting Json to Recording for request=%s, message= %s", request, e.getMessage()));
                return Optional.empty();
            }
        }
        else {
            log.error(String.format("Error while retrieving the data for request=%s, statusCode=%s, message=%s", request, response.getStatusCode(), response.getBody()));
            return Optional.empty();
        }
    }

    public <T> ResponseEntity fetchPostResponseForUserHistory(HttpServletRequest request,
            String customerId, String app, String instance, Optional<T> formParams) {
        String userHistoryUrl =
            "/cs/start/" + customerId+ "/" + app + "/" + instance + "/" + "Default" + app;
        return fetchPostResponse(request, formParams, userHistoryUrl,
                MediaType.APPLICATION_FORM_URLENCODED);
    }

    public Optional<List<ApiTraceResponse>> getApiTrace(HttpServletRequest request, String customerId, String app) {
        String path = cubeServerBaseUrlReplay + String.format("/as/getApiTrace/%s/%s", customerId, app);
        ResponseEntity response = fetchGetResponse(path, request.getQueryString());
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                final String body = response.getBody().toString();
                JsonNode json = jsonMapper.readTree(body);
                JsonNode responseBody = json.get("response");
                ObjectReader reader = jsonMapper.readerFor(new TypeReference<List<ApiTraceResponse>>() {
                });
                final List<ApiTraceResponse> apiTraceResponses = reader.readValue(responseBody);
                return Optional.of(apiTraceResponses);
            } catch (Exception e) {
                log.info(String.format("Error in converting Json to ApiTraceResponse for customerId=%s, app=%s, message= %s",
                    customerId, app, e.getMessage()));
                return Optional.empty();
            }
        }
        else {
            log.error(String.format("Error while retrieving the data from path=%s , statusCode=%s, message=%s",
                path, response.getStatusCode(), response.getBody()));
            return Optional.empty();
        }
    }

    public String getPathForHttpMethod(String uri , String method , String... lastParams){
        String path = String.join("/" ,  lastParams);
        return uri.replace(path , path + "/" + method).replace("/api" , "");
    }

    public <T> ResponseEntity fetchGetResponse(HttpServletRequest request, Optional<T> requestBody, String... path) {
        return fetchResponse(request, requestBody, HttpMethod.GET, path);
    }

    public <T> ResponseEntity fetchPostResponse(HttpServletRequest request, Optional<T> requestBody, String... path) {
        return fetchResponse(request, requestBody, HttpMethod.POST, path);
    }

    public <T> ResponseEntity fetchResponse(HttpServletRequest request, Optional<T> requestBody, HttpMethod method, String... pathValue){
        String requestURI = pathValue.length> 0 ? pathValue[0] : request.getRequestURI().replace("/api", "");
        String path = getCubeServerUrl(requestURI);
        if (request.getQueryString() != null) {
            path += "?" + request.getQueryString();
        }
        try {
            // here escaping is not needed, since the getRequestURI returns escaped. So using regular URI constructor
            URI uri = new URI(path);
            HttpHeaders headers = new HttpHeaders();
            request.getHeaderNames().asIterator().forEachRemaining(key -> headers.set(key, request.getHeader(key)));
            if (pathValue.length >1)
                headers.set("Content-Type", pathValue[1]);
//            MultiValueMap<String, String[]> map = new LinkedMultiValueMap<>();
//            request.getParameterMap().forEach(map::add);
            HttpEntity<T> entity;
            entity = requestBody.map(body -> new HttpEntity<>(body, headers)).orElseGet(() -> new HttpEntity<>(headers));
//            return restTemplate.postForEntity(uri, entity, String.class);
            return restTemplate.exchange(uri, method, entity, String.class);
        } catch (URISyntaxException e){
            return noContent().build();
        } catch (HttpClientErrorException e){
            return status(e.getStatusCode()).body(e.getResponseBodyAsByteArray());
        } catch(HttpServerErrorException e) {
            return status(e.getStatusCode()).body(e.getResponseBodyAsByteArray());
        } catch(UnknownHttpStatusCodeException e) {
            return status(e.getRawStatusCode()).body(e.getResponseBodyAsByteArray());
        } catch (Exception e){
            return status(NOT_FOUND).body(e);
        }
    }

    private String getCubeServerUrl(String uri) {
        if (uri.startsWith("/as") || uri.startsWith("/rs"))
            return cubeServerBaseUrlReplay.concat(uri);
        else if (uri.startsWith("/ms"))
            return  cubeServerBaseUrlMock.concat(uri);
        else if (uri.startsWith("/cs"))
            return cubeServerBaseUrlRecord.concat(uri);
        return "";
    }
}
