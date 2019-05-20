package com.cubeui.backend.web.rest;

import com.cubeui.backend.web.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.net.URISyntaxException;

import static com.cubeui.backend.security.Constants.CUBE_SERVER_HREF;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.*;
import static org.springframework.http.ResponseEntity.status;

@RestController
@RequestMapping("/ms")
public class MockServiceController {

    private RestTemplate restTemplate;
    private String baseHref =  CUBE_SERVER_HREF  + "/ms";

    public MockServiceController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity health() {
        return fetchGetResponse("/health");
    }


    @GetMapping("/{customerid}/{app}/{instanceid}/{service}/{var:.+}")
    public ResponseEntity get(@PathVariable("var") String path,
//                              @Context HttpHeaders headers
                        @PathVariable("customerid") String customerid,
                        @PathVariable("app") String app,
                        @PathVariable("instanceid") String instanceid,
                        @PathVariable("service") String service) {
        return fetchGetResponse("/"+customerid+"/"+app+"/"+instanceid+"/"+service+"/"+path/*var:.+}"*/);
    }

    @PostMapping("/{customerid}/{app}/{instanceid}/{service}/{var:.+}")
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseEntity postForms(@PathVariable("var") String path,
                                    MultivaluedMap<String, String> formParams,
//                                   @Context HttpHeaders headers,
                                    @PathVariable("customerid") String customerid,
                                    @PathVariable("app") String app,
                                    @PathVariable("instanceid") String instanceid,
                                    @PathVariable("service") String service) {
        return noContent().build();
    }

    @PostMapping("/{customerid}/{app}/{instanceid}/{service}/{var:.+}")
//    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseEntity postJson(@PathVariable("var") String path,
                                   @PathVariable("customerid") String customerid,
                                   @PathVariable("app") String app,
                                   @PathVariable("instanceid") String instanceid,
                                   @PathVariable("service") String service,
//                                   @Context HttpHeaders headers,
                                   String body) {
        return noContent().build();
    }

    @PostMapping("/fr")
//    @Consumes(MediaType.TEXT_PLAIN)
    public ResponseEntity funcJson(String fnReqResponseAsString) {
        return noContent().build();
    }

    private ResponseEntity fetchGetResponse(String path){
        String urlString = baseHref + path;
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
