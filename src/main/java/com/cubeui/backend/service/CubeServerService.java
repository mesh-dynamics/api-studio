package com.cubeui.backend.service;

import com.cubeui.backend.web.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

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

    public ResponseEntity fetchGetResponse(String path){
        String urlString = CUBE_SERVER_HREF + path;
        try {
            URI uri = new URI(urlString);
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
}
