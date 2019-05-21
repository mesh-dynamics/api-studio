package com.cubeui.backend.web.rest;

import com.cubeui.backend.service.CubeServerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;

import static com.cubeui.backend.security.Constants.CUBE_SERVER_HREF;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/cs")
public class CubeStoreController {


    private CubeServerService cubeServerService;
    private RestTemplate restTemplate;
    private String baseHref =  CUBE_SERVER_HREF + "/cs";

    public CubeStoreController(CubeServerService cubeServerService, RestTemplate restTemplate) {
        this.cubeServerService = cubeServerService;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity getData1(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @GetMapping("/status/{customerid}/{app}/{collection}")
    public ResponseEntity status(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @GetMapping("/recordings")
    public ResponseEntity recordings(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @GetMapping("/currentcollection")
    public ResponseEntity currentcollection(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @PostMapping(value = "/req")
    public ResponseEntity storereq(@RequestBody String req) { // Edited: Request req
        return noContent().build();
    }

    @PostMapping("/resp")
    public ResponseEntity storeresp(@RequestBody String resp) { // Edited: Response resp
        return noContent().build();
    }

    @PostMapping(value = "/fr", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity storeFunc(String functionReqRespString /* @PathParam("customer") String customer,
                              @PathParam("instance") String instance, @PathParam("app") String app,
                              @PathParam("service") String service*/) {
        return noContent().build();
    }

    @PostMapping(value = "/setdefault/{customerid}/{app}/{serviceid}/{method}/{var:.+}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity setDefault(
//            @Context UriInfo ui,
            @PathVariable("var") String path,
//            MultivaluedMap<String, String> formParams,
            @PathVariable("customerid") String customerid,
            @PathVariable("app") String app,
            @PathVariable("serviceid") String serviceid,
            @PathVariable("method") String method) {
        return noContent().build();
    }

    @PostMapping("/setdefault/{method}/{var:.+}")
    public ResponseEntity setDefaultFullResp(
//            @Context UriInfo ui,
            @PathVariable("var") String path,
            @RequestBody String resp, // Edited: com.cube.dao.Response resp,
            @PathVariable("method") String method) {
        return noContent().build();
    }

    @PostMapping(value = "/start/{customerid}/{app}/{instanceid}/{collection}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity start(
//            @Context UriInfo ui,
                          @PathVariable("app") String app,
                          @PathVariable("customerid") String customerid,
                          @PathVariable("instanceid") String instanceid,
                          @PathVariable("collection") String collection) {
        return noContent().build();
    }

    @PostMapping("/stop/{customerid}/{app}/{collection}")
    public ResponseEntity stop(
//            @Context UriInfo ui,
            @PathVariable("collection") String collection,
            @PathVariable("customerid") String customerid,
            @PathVariable("app") String app) {
        return noContent().build();
    }

}
