package com.cubeui.backend.web.external;

import com.cubeui.backend.service.CubeServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

@RestController
@RequestMapping("/api/as")
public class AnalyzeWSController {

    private CubeServerService cubeServerService;

    public AnalyzeWSController(CubeServerService cubeServerService) {
        this.cubeServerService = cubeServerService;
    }

    @GetMapping("**")
    public ResponseEntity getData(HttpServletRequest request, @RequestBody Optional<String> getBody) {
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("**")
    public ResponseEntity postData(HttpServletRequest request, @RequestBody Optional<String> postBody) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }
}
