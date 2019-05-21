package com.cubeui.backend.service;

import com.cubeui.backend.web.ErrorResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static com.cubeui.backend.security.Constants.CUBE_SERVER_HREF;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.*;

@Service
@Transactional
public class CubeServerService {

    private RestTemplate restTemplate;

    public CubeServerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

//    public ResponseEntity fetchGetResponse(HttpServletRequest request){
//        String path = CUBE_SERVER_HREF + request.getRequestURI() + "?";
//        if (request.getQueryString() != null) {
//            path += request.getQueryString();
//        }
//        return fetchGetResponse(path);
//    }

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

    public ResponseEntity fetchGetResponse(HttpServletRequest request) {
        return fetchResponse(request, Optional.empty(), HttpMethod.GET);
    }

    public ResponseEntity fetchPostResponse(HttpServletRequest request, Optional<String> requestBody) {
        return fetchResponse(request, requestBody, HttpMethod.POST);
    }

    private ResponseEntity fetchResponse(HttpServletRequest request, Optional<String> requestBody, HttpMethod method){
        String path = CUBE_SERVER_HREF + request.getRequestURI() + "?";
        if (request.getQueryString() != null) {
            path += request.getQueryString();
        }
        try {
            URI uri = new URI(path);
            HttpHeaders headers = new HttpHeaders();
            Iterator<String> it = request.getHeaderNames().asIterator();
            while (it.hasNext()) {
                String key = it.next();
                headers.set(key, request.getHeader(key));
            }
//            MultiValueMap<String, String[]> map = new LinkedMultiValueMap<>();
//            Map<String, String[]> params = request.getParameterMap();
//            for (Map.Entry<String, String[]> entry : params.entrySet()){
//                map.add(entry.getKey(), entry.getValue());
//            }
            HttpEntity entity;
            if (requestBody.isPresent()){
                entity = new HttpEntity(requestBody.get(), headers);
            } else {
                entity = new HttpEntity(headers);
            }
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
