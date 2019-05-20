package com.cubeui.backend.web.rest;

import com.cubeui.backend.service.CubeServerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.http.ResponseEntity.noContent;

@RestController
@RequestMapping("/rs")
public class ReplayWSController {

    private CubeServerService cubeServerService;
    private String baseHref =  "/rs";

    public ReplayWSController(CubeServerService cubeServerService) {
        this.cubeServerService = cubeServerService;
    }

    @GetMapping("/health")
    public ResponseEntity getData1(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(baseHref + "/health");
    }

    @GetMapping("/status/{customerid}/{app}/{collection}/{replayid}")
    public ResponseEntity status(
//            @Context UriInfo ui,
            @PathVariable("collection") String collection,
            @PathVariable("replayid") String replayid,
            @PathVariable("customerid") String customerid,
            @PathVariable("app") String app) {
        return cubeServerService.fetchGetResponse(baseHref + "/status/" + customerid + "/" + app + "/" + collection + "/" + replayid);
    }

    @PostMapping(value = "/init/{customerid}/{app}/{collection}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity init(
//                        @Context UriInfo ui,
                         @PathVariable("collection") String collection,
//                         MultivaluedMap<String, String> formParams,
                         @PathVariable("customerid") String customerid,
                         @PathVariable("app") String app) {
        return noContent().build();
    }

    @PostMapping(value = "transforms/{customerid}/{app}/{collection}/{replayid}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity upsertTransforms(
//                                    @Context UriInfo ui,
//                                     MultivaluedMap<String, String> formParams,
                                     @PathVariable("customerid") String customerid,
                                     @PathVariable("app") String app,
                                     @PathVariable("collection") String collection,
                                     @PathVariable("replayid") String replayid) {
        return noContent().build();
    }

    @PostMapping("forcecomplete/{replayid}")
    public ResponseEntity forceComplete(
//            @Context UriInfo ui,
            @PathVariable("replayid") String replayid) {
        return noContent().build();
    }

    @PostMapping(value = "start/{customerid}/{app}/{collection}/{replayid}", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity start(
//            @Context UriInfo ui,
            @PathVariable("collection") String collection,
            @PathVariable("replayid") String replayid,
            @PathVariable("customerid") String customerid,
            @PathVariable("app") String app) {
//            MultivaluedMap<String, String> formParams) {
        return noContent().build();
    }
}
