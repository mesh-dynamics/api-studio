package com.cubeui.backend.web.rest;

import com.cubeui.backend.service.CubeServerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.http.ResponseEntity.noContent;

@RestController
@RequestMapping("/cs")
public class CubeStoreController {


    private CubeServerService cubeServerService;
    private String baseHref =  "/cs";

    public CubeStoreController(CubeServerService cubeServerService) {
        this.cubeServerService = cubeServerService;
    }

    @GetMapping("/health")
    public ResponseEntity getData1(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(baseHref + "/health");
    }

    @GetMapping("/status/{customerid}/{app}/{collection}")
    public ResponseEntity status(
//            @Context UriInfo ui,
            @PathVariable("collection") String collection,
            @PathVariable("customerid") String customerid,
            @PathVariable("app") String app) {
        return cubeServerService.fetchGetResponse(baseHref + "/status/" + customerid + "/" + app + "/" + collection);
    }

    @GetMapping("/recordings")
    public ResponseEntity recordings(/*@Context UriInfo ui*/) {
        return cubeServerService.fetchGetResponse(baseHref + "/recordings");
    }

    @GetMapping("/currentcollection")
    public ResponseEntity currentcollection(/*@Context UriInfo ui*/) {
        return cubeServerService.fetchGetResponse(baseHref + "/currentcollection");
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
