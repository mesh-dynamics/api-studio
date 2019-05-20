package com.cubeui.backend.web.rest;

import com.cubeui.backend.service.CubeServerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/ms")
public class MockServiceController {

    private String baseHref =  "/ms";
    private CubeServerService cubeServerService;

    public MockServiceController(CubeServerService cubeServerService) {
        this.cubeServerService = cubeServerService;
    }

    @GetMapping("/health")
    public ResponseEntity health() {
        return cubeServerService.fetchGetResponse(baseHref + "/health");
    }


    @GetMapping("/{customerid}/{app}/{instanceid}/{service}/{var:.+}")
    public ResponseEntity get(@PathVariable("var") String path,
//                              @Context HttpHeaders headers
                        @PathVariable("customerid") String customerid,
                        @PathVariable("app") String app,
                        @PathVariable("instanceid") String instanceid,
                        @PathVariable("service") String service) {
        return cubeServerService.fetchGetResponse(baseHref + "/"+customerid+"/"+app+"/"+instanceid+"/"+service+"/"+path/*var:.+}"*/);
    }

    @PostMapping(value = "/{customerid}/{app}/{instanceid}/{service}/{var:.+}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity postForms(@PathVariable("var") String path,
                                    MultivaluedMap<String, String> formParams,
                                    @Context HttpHeaders headers,
                                    @PathVariable("customerid") String customerid,
                                    @PathVariable("app") String app,
                                    @PathVariable("instanceid") String instanceid,
                                    @PathVariable("service") String service) {
        return noContent().build();
    }

    @PostMapping(value = "/{customerid}/{app}/{instanceid}/{service}/{var:.+}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity postJson(@PathVariable("var") String path,
                                   @PathVariable("customerid") String customerid,
                                   @PathVariable("app") String app,
                                   @PathVariable("instanceid") String instanceid,
                                   @PathVariable("service") String service,
//                                   @Context HttpHeaders headers,
                                   String body) {
        return noContent().build();
    }

    @PostMapping(value = "/fr", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity funcJson(String fnReqResponseAsString) {
        return noContent().build();
    }

}
