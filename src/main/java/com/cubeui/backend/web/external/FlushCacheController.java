package com.cubeui.backend.web.external;

import com.cubeui.backend.service.CubeServerService;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
public class FlushCacheController {

  @Autowired
  private CubeServerService cubeServerService;

  @PostMapping("/flushall")
  public ResponseEntity cacheFlushAll(HttpServletRequest request, @RequestBody Optional<String> postBody) {
    cubeServerService.fetchPostResponse(request, postBody, "/cs/cache/flushall");
    cubeServerService.fetchPostResponse(request,postBody, "/rs/cache/flushall");
    return cubeServerService.fetchPostResponse(request,postBody,"/as/cache/flushall");
  }
}
