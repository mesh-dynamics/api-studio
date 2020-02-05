package com.cubeui.backend.service;

import com.cubeui.backend.web.ErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static com.cubeui.backend.security.Constants.CUBE_SERVER_HREF;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.*;

@Service
@Transactional
public class CubeServerService {

    @Value("${cube.server.baseUrl}")
    private String cubeServerBaseUrl = CUBE_SERVER_HREF;

    private RestTemplate restTemplate;

    public CubeServerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity fetchGetResponse(String path){
        try {
            URI uri = new URI(path);
            String result = restTemplate.getForObject(uri, String.class);
            return ok().body(result);
        } catch (URISyntaxException e){
            return noContent().build();
        } catch (HttpClientErrorException e){
            return status(e.getStatusCode()).body(new ErrorResponse(e.getLocalizedMessage()));
        } catch (Exception e){
            return status(NOT_FOUND).body(e);
        }
    }

    public <T> ResponseEntity fetchGetResponse(HttpServletRequest request, Optional<T> requestBody) {
        return fetchResponse(request, requestBody, HttpMethod.GET);
    }

    public <T> ResponseEntity fetchPostResponse(HttpServletRequest request, Optional<T> requestBody) {
        return fetchResponse(request, requestBody, HttpMethod.POST);
    }

    private <T> ResponseEntity fetchResponse(HttpServletRequest request, Optional<T> requestBody, HttpMethod method){
        String path = cubeServerBaseUrl + request.getRequestURI().replace("/api", "");
        if (request.getQueryString() != null) {
            path += "?" + request.getQueryString();
        }
        try {
            URI uri = new URI(path);
            HttpHeaders headers = new HttpHeaders();
            request.getHeaderNames().asIterator().forEachRemaining(key -> headers.set(key, request.getHeader(key)));
//            MultiValueMap<String, String[]> map = new LinkedMultiValueMap<>();
//            request.getParameterMap().forEach(map::add);
            HttpEntity<T> entity;
            entity = requestBody.map(body -> new HttpEntity<>(body, headers)).orElseGet(() -> new HttpEntity<>(headers));
//            return restTemplate.postForEntity(uri, entity, String.class);
            return restTemplate.exchange(uri, method, entity, String.class);
        } catch (URISyntaxException e){
            return noContent().build();
        } catch (HttpClientErrorException e){
            return status(e.getStatusCode()).body(new ErrorResponse(e.getLocalizedMessage()));
        } catch (Exception e){
            return status(NOT_FOUND).body(e);
        }
    }
}
