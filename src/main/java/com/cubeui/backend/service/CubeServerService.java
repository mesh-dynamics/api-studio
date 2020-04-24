package com.cubeui.backend.service;

import io.md.dao.Replay;
import com.cubeui.backend.web.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    private String cubeServerBaseUrl = CUBE_SERVER_HREF;

    @Autowired
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

    public ResponseEntity fetchGetResponse(String path){
        try {
            URI uri = new URI(null, null, null, 0, path, null, null);
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
        final ResponseEntity  response = fetchGetResponse(path);
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

    public <T> ResponseEntity fetchGetResponse(HttpServletRequest request, Optional<T> requestBody, String... path) {
        return fetchResponse(request, requestBody, HttpMethod.GET, path);
    }

    public <T> ResponseEntity fetchPostResponse(HttpServletRequest request, Optional<T> requestBody, String... path) {
        return fetchResponse(request, requestBody, HttpMethod.POST, path);
    }

    private <T> ResponseEntity fetchResponse(HttpServletRequest request, Optional<T> requestBody, HttpMethod method, String... pathValue){
        updateCubeBaseUrl(request);
        String path = cubeServerBaseUrl + (pathValue.length> 0 ? pathValue[0] : request.getRequestURI().replace("/api", ""));
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

    private void updateCubeBaseUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.contains("/api/as/") || uri.contains("/api/rs"))
            cubeServerBaseUrl = cubeServerBaseUrlReplay;
        else if (uri.contains("api/ms/"))
            cubeServerBaseUrl = cubeServerBaseUrlMock;
        else if (uri.contains("/api/cs/"))
            cubeServerBaseUrl = cubeServerBaseUrlRecord;
    }
}
