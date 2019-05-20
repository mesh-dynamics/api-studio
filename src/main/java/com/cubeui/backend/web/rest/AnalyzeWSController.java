package com.cubeui.backend.web.rest;

import com.cubeui.backend.web.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.net.URISyntaxException;

import static com.cubeui.backend.security.Constants.CUBE_SERVER_HREF;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.*;
import static org.springframework.http.ResponseEntity.status;

@RestController
@RequestMapping("/as")
public class AnalyzeWSController {

    private RestTemplate restTemplate;
    private String baseHref =  CUBE_SERVER_HREF  + "/as";

    public AnalyzeWSController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity getData1(HttpServletRequest request) {
        return fetchGetResponse("/health");
    }

    @GetMapping("/aggrresult/{replayid}")
    public ResponseEntity getResultAggregate(@PathVariable("replayid") String replayid) {
        return fetchGetResponse("/aggrresult/" + replayid);
    }

    @GetMapping("/replayRes/{customerId}/{app}/{service}/{replayId}")
    public ResponseEntity replayResult(@PathVariable("customerId") String customerId,
                                       @PathVariable("app") String app,
                                       @PathVariable("service") String service,
                                       @PathVariable("replayId") String replayId) {
        return fetchGetResponse("/replayRes/" + customerId +"/" + app + "/" + service + "/" + replayId);
    }

    @GetMapping("/analysisRes/{replayId}/{recordReqId}")
    public ResponseEntity getAnalysisResult(@PathVariable("recordReqId") String recordReqId,
                                            @PathVariable("replayId") String replayId) {
        return fetchGetResponse("/analysisRes/" + replayId + "/" + recordReqId);
    }

    @GetMapping("/timelineres/{customer}/{app}/{instanceId}")
    public ResponseEntity getTimelineResults(@PathVariable("customer") String customer,
                                             @PathVariable("app") String app,
                                             @PathVariable("instanceId") String instanceId) {
        return fetchGetResponse("/timelineres/" + customer + "/" + app + "/" + instanceId);
    }

    @PostMapping("registerTemplate/{type}/{customerId}/{appId}/{serviceName}/{path:.+}")
//    @Consumes({MediaType.APPLICATION_JSON})
    public ResponseEntity registerTemplate(@PathVariable("appId") String appId,
                                           @PathVariable("customerId") String customerId,
                                           @PathVariable("serviceName") String serviceName,
                                           @PathVariable("path") String path,
                                           @PathVariable("type") String type,
                                           String templateAsJson) {
        return noContent().build();
    }

    @PostMapping("registerTemplateApp/{type}/{customerId}/{appId}")
//    @Consumes({MediaType.APPLICATION_JSON})
    public ResponseEntity registerTemplateApp(@PathVariable("type") String type,
                                              @PathVariable("customerId") String customerId ,
                                              @PathVariable("appId") String appId,
                                              String templateRegistryArray) {
        return noContent().build();
    }

    @PostMapping("/analyze/{replayid}")
//    @Consumes("application/x-www-form-urlencoded")
    public ResponseEntity analyze(@PathVariable("replayid") String replayid, MultivaluedMap<String, String> formParams) {
        return fetchGetResponse("/analyze/" + replayid);
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
