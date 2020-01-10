package com.cubeui.backend.web.external;

import com.cubeui.backend.service.CubeServerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

@RestController
@RequestMapping("/ms")
public class MockServiceController {

    private CubeServerService cubeServerService;

    public MockServiceController(CubeServerService cubeServerService) {
        this.cubeServerService = cubeServerService;
    }
    @GetMapping("**")
    public ResponseEntity getData(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @PostMapping("**")
    public ResponseEntity postData(HttpServletRequest request, Optional<String> postBody) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

}
