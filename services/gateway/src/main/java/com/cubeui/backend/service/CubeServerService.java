/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cubeui.backend.service;

import static com.cubeui.backend.security.Constants.CUBE_SERVER_HREF;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.Response.AppResponse;
import com.cubeui.backend.web.ErrorResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.md.core.AttributeRuleMap;
import io.md.dao.ApiTraceResponse;
import io.md.dao.CompareTemplateVersioned;
import io.md.dao.Event;
import io.md.dao.EventQuery;
import io.md.dao.Recording;
import io.md.dao.Replay;
import io.md.dao.TemplateSet;
import io.md.utils.Constants;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;

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

    @Value("${cube.server.port:}")
    private String cubeServerPort = "";

    public String getUrls(){
        return String.format("%s-%s",cubeServerBaseUrlReplay , cubeServerPort );
    }

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

        Optional<Integer>  cubeport =  io.md.utils.Utils.strToInt(cubeServerPort);
        if(cubeport.isPresent()){
            //If a specific cubeio port is provided , replace the port in all urls
            cubeServerBaseUrlReplay = cubeServerBaseUrlReplay.replaceFirst(":\\d+" , ":"+cubeServerPort);
            cubeServerBaseUrlMock = cubeServerBaseUrlMock.replaceFirst(":\\d+" , ":"+cubeServerPort);
            cubeServerBaseUrlRecord = cubeServerBaseUrlRecord.replaceFirst(":\\d+" , ":"+cubeServerPort);

        }
    }

    public ResponseEntity fetchGetResponse(String path, String query){
        try {
            URI uri = new URI(null, null, null, 0, path, query, null);
            byte[] result = restTemplate.getForObject(uri, byte[].class);
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
        return getData(response, path, Replay.class);
    }

    public <T> Optional<T> getData(ResponseEntity<byte[]> response, String path, Class<T> valueType) {
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                final String body = new String(response.getBody());
                final T data = jsonMapper.readValue(body, valueType);
                return Optional.of(data);
            } catch (Exception e) {
                log.info("Error in converting Json to value=" + valueType + ", path="  + path + " message"  + e.getMessage());
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
        return getData(response, path, Recording.class);
    }

    public List<AppResponse> getAppResponse(ResponseEntity<byte[]> responseEntity, List<App> apps) {
        List<AppResponse> response = new ArrayList<>();
        try {
            String body = new String(responseEntity.getBody());
            JsonNode json = jsonMapper.readTree(body);
            for (App app : apps) {
                AppResponse appResponse = new AppResponse();
                appResponse.setApp(app);
                JsonNode responseBody = json.get(app.getName());
                appResponse.setConfiguration(responseBody);
                response.add(appResponse);
            }
        }catch (Exception e) {
            log.info(String.format("Error in converting Json to Map for message= %s", e.getMessage()));
        }
        return response;
    }

    public <T> Optional<List<T>> getListData(ResponseEntity<byte[]> response, String request, Optional<String> getField, TypeReference typeReference) {
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                String body = new String(response.getBody());
                if(getField.isPresent()) {
                    JsonNode json = jsonMapper.readTree(body);
                    JsonNode responseBody = json.get(getField.get());
                    body = responseBody.toString();
                }
                List<T> data = jsonMapper.readValue(body, typeReference);
                return Optional.of(data);
            } catch (Exception e) {
                log.info(String.format("Error in converting Json to response List for request=%s, message= %s", request, e.getMessage()));
                return Optional.empty();
            }
        }
        else {
            log.error(String.format("Error while retrieving the data for request=%s, statusCode=%s, message=%s", request, response.getStatusCode(), response.getBody()));
            return Optional.empty();
        }
    }
    
    public Optional<List<Event>> getEvents(EventQuery query, HttpServletRequest request) {
        ResponseEntity response = fetchPostResponse(request, Optional.of(query), "/cs/getEvents");
        return getListData(response,"/cs/getEvents", Optional.of("objects"), new TypeReference<List<Event>>(){});
    }

    public Optional<Recording> searchRecording(String query) {
        String path = cubeServerBaseUrlRecord + "/cs/searchRecording";
        ResponseEntity response = fetchGetResponse(path, query);
        Optional<List<Recording>> recordings = getListData(response, path+query, Optional.of("recordings"), new TypeReference<List<Recording>>(){});
        return recordings.map(r -> r.stream().findFirst()).orElse(Optional.empty());
    }

    @Async("threadPoolTaskExecutor")
    public void saveEmptyTemplateSetForApp(HttpServletRequest request, App app) {
        URI uri = UriBuilder
            .fromPath("/as/saveTemplateSet/{customer}/{app}")
            .build(app.getCustomer().getName(), app.getName());
        TemplateSet templateSet = new TemplateSet(app.getCustomer().getName(),
            app.getName(), null, Collections.emptyList(), Optional.empty(), "Default"+app.getName(), "");
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("file",  templateSet);
        fetchPostResponse(request, Optional.of(map), uri.getPath());
    }

    public ResponseEntity getTemplateSetLabels(App app) {
        URI uri = UriBuilder
            .fromPath("/as/getTemplateSetLabels/{customerId}/{appId}")
            .queryParam(Constants.TEMPLATE_SET_NAME, "Default"+app.getName())
            .queryParam(Constants.TEMPLATE_SET_LABEL, "")
            .build(app.getCustomer().getName(), app.getName());
        return fetchGetResponse(cubeServerBaseUrlReplay + uri.getPath(), uri.getQuery());
    }

    public ResponseEntity getTimeLinersResult(Replay replay) {
        UriBuilder uriBuilder = UriBuilder
            .fromPath("/as/timelineres/{customerId}/{app}")
            .queryParam(Constants.GOLDEN_NAME_FIELD, replay.goldenName.get())
            .queryParam(Constants.NUM_RESULTS_FIELD, 30)
            .queryParam(Constants.USER_ID_FIELD, replay.userId)
            .queryParam("byPath", "y")
            .queryParam(Constants.END_DATE_FIELD, replay.creationTimeStamp.toString());
        if(replay.testConfigName.isPresent()) {
            uriBuilder.queryParam(Constants.TEST_CONFIG_NAME_FIELD, replay.testConfigName.get());
        }
        URI uri = uriBuilder.build(replay.customerId, replay.app);
        return fetchGetResponse(cubeServerBaseUrlReplay + uri.getPath(), uri.getQuery());
    }

    public Optional<Recording> getRecordingFromResponseEntity(ResponseEntity response, String request) {
        return getData(response, request, Recording.class);
    }

    public <T> ResponseEntity createRecording(HttpServletRequest request,
            String customerId, String app, String instance, Optional<T> formParams) {
        String userHistoryUrl =
            "/cs/start/" + customerId+ "/" + app + "/" + instance + "/" + "Default" + app;
        return fetchPostResponse(request, formParams, userHistoryUrl,
                MediaType.APPLICATION_FORM_URLENCODED);
    }

    public Optional<List<ApiTraceResponse>> getApiTrace(HttpServletRequest request, String customerId, String app) {
        String path = cubeServerBaseUrlReplay + String.format("/as/getApiTrace/%s/%s", customerId, app);
        ResponseEntity response = fetchGetResponse(path, request.getQueryString());
        Optional<List<ApiTraceResponse>> apiTraceResponses = getListData(response, path+request.getQueryString(),
            Optional.of("response"), new TypeReference<List<ApiTraceResponse>>(){});
        return apiTraceResponses;
    }

    public String getPathForHttpMethod(String uri , String method , String... lastParams){
        String path = URLDecoder.decode(String.join("/" ,  lastParams), Charset.defaultCharset());
        String decodedUri = URLDecoder.decode(uri, Charset.defaultCharset());
        return decodedUri.replace(path , path + "/" + method).replaceFirst("^/api" , "");
    }

    public <T> ResponseEntity fetchGetResponse(HttpServletRequest request, Optional<T> requestBody, String... path) {
        return fetchResponse(request, requestBody, HttpMethod.GET, path);
    }

    public <T> ResponseEntity fetchPostResponse(HttpServletRequest request, Optional<T> requestBody, String... path) {
        return fetchResponse(request, requestBody, HttpMethod.POST, path);
    }

    public <T> ResponseEntity fetchResponse(HttpServletRequest request, Optional<T> requestBody, HttpMethod method, String... pathValue){
        // NOTE: request param is null for internal calls.

        String requestURI = pathValue.length > 0 ? pathValue[0]
            : request != null ? request.getRequestURI().replaceFirst("^/api", "") : null;

        if (requestURI == null)
        {
            log.error("URI not found in request or pathValue");
            return noContent().build();
        }

        String path = getCubeServerUrl(requestURI);

        if (request != null && request.getQueryString() != null) {
            path += "?" + request.getQueryString();
        }
        try {
            // here escaping is not needed, since the getRequestURI returns escaped. So using regular URI constructor
            URI uri = new URI(path);
            HttpHeaders headers = new HttpHeaders();
            if (request != null) {
                request.getHeaderNames().asIterator()
                    .forEachRemaining(key -> headers.set(key, request.getHeader(key)));
            }
            if (pathValue.length >1)
                headers.set("Content-Type", pathValue[1]);
//            MultiValueMap<String, String[]> map = new LinkedMultiValueMap<>();
//            request.getParameterMap().forEach(map::add);
            HttpEntity<T> entity;
            entity = requestBody.map(body -> new HttpEntity<>(body, headers)).orElseGet(() -> new HttpEntity<>(headers));
//            return restTemplate.postForEntity(uri, entity, String.class);
            ResponseEntity<byte[]> response = restTemplate
                .exchange(uri, method, entity, byte[].class);
            return response;
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
