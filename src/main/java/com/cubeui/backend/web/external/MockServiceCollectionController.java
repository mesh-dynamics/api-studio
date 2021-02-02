package com.cubeui.backend.web.external;

import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import io.md.dao.Recording;
import java.util.Base64;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/msc")
@Slf4j
public class MockServiceCollectionController {

  @Autowired
  private CubeServerService cubeServerService;
  @Autowired
  private Validation validation;

  @RequestMapping(value = "/mock/{replayCollection}/{recordCollection}/{customerId}/{app}/{traceId}/{service}/**" , consumes = {MediaType.ALL_VALUE})
  public ResponseEntity mockWithCollection(HttpServletRequest request, @RequestBody Optional<String> body,
      @PathVariable String replayCollection, @PathVariable String recordCollection,
      @PathVariable String customerId, @PathVariable String app,
      @PathVariable String traceId, @PathVariable String service, Authentication authentication) {
    validation.validateCustomerName(authentication,customerId);
    String query =  String.format("customerId=%s&app=%s&collection=%s", customerId, app, recordCollection);
    Optional<Recording> recording = cubeServerService.searchRecording(query);
    if(recording.isEmpty())
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(String.format("There is no Recording Object for customerId=%s, app=%s, collection=%s",
              customerId, app,  recordCollection));
    validation.validateCustomerName(authentication,recording.get().customerId);

    String path = getPath(request.getRequestURI(), replayCollection, recordCollection, customerId, app, recording.get().id);
    body.ifPresent(b -> log.info("Encoded Body", Base64.getEncoder().encode(b.getBytes())));
    path = cubeServerService.getPathForHttpMethod(path , request.getMethod()  , traceId, service);

    return cubeServerService.fetchResponse(request, body, HttpMethod.POST , path);
  }


  @RequestMapping(value = "/mockWithRunId/{replayCollection}/{recordCollection}/{customerId}/{app}/{timestamp}/{traceId}/{runId}/{service}/**" , consumes = {MediaType.ALL_VALUE})
  public ResponseEntity mockWithRunId(HttpServletRequest request, @RequestBody Optional<String> body,
      @PathVariable String replayCollection, @PathVariable String recordCollection,
      @PathVariable String customerId, @PathVariable String app, @PathVariable String timestamp,
      @PathVariable String traceId, @PathVariable String service, @PathVariable String runId, Authentication authentication) {
    validation.validateCustomerName(authentication,customerId);
    String query =  String.format("customerId=%s&app=%s&collection=%s", customerId, app, recordCollection);
    Optional<Recording> recording = cubeServerService.searchRecording(query);
    body.ifPresent(b -> log.info("Encoded Body", Base64.getEncoder().encode(b.getBytes())));
    if(recording.isEmpty())
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(String.format("There is no Recording Object for customerId=%s, app=%s, collection=%s",
              customerId, app,  recordCollection));
    validation.validateCustomerName(authentication,recording.get().customerId);

    String path = getPathForMockWithRunId(request.getRequestURI(), replayCollection, recordCollection, customerId, app, recording.get().id);
    path = cubeServerService.getPathForHttpMethod(path , request.getMethod() , traceId, runId, service, timestamp);

    return cubeServerService.fetchResponse(request, body, HttpMethod.POST , path);
  }

  private String getPath(String uri, String replayCollection, String recordCollection,
      String customerId, String app,String recordingId) {
    return uri.replace(String.format("/api/msc/mock/%s/%s/%s/%s",
        replayCollection, recordCollection, customerId, app),
        String.format("/ms/mockWithCollection/%s/%s", replayCollection, recordingId ));
  }

  private String getPathForMockWithRunId(String uri, String replayCollection, String recordCollection,
      String customerId, String app,String recordingId) {
    return uri.replace(String.format("/api/msc/mockWithRunId/%s/%s/%s/%s",
        replayCollection, recordCollection, customerId, app),
        String.format("/ms/mockWithRunId/%s/%s", replayCollection, recordingId));
  }
}
