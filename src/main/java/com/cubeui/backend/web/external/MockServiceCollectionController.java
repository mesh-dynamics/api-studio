package com.cubeui.backend.web.external;

import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import io.md.dao.Recording;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/msc")
public class MockServiceCollectionController {

  @Autowired
  private CubeServerService cubeServerService;
  @Autowired
  private Validation validation;

  @GetMapping("/mock/{replayCollection}/{recordCollection}/{customerId}/{app}/{instanceId}/{service}/**")
  public ResponseEntity getData(HttpServletRequest request, @RequestBody Optional<String> getBody,
      @PathVariable String replayCollection, @PathVariable String recordCollection,
      @PathVariable String customerId, @PathVariable String app,
      @PathVariable String instanceId, @PathVariable String service) {
    validation.validateCustomerName(request,customerId);
    Optional<Recording> recording = cubeServerService.searchRecording(customerId, app, recordCollection);
    if(recording.isEmpty())
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(String.format("There is no Recording Object for customerId=%s, app=%s, collection=%s",
              customerId, app,  recordCollection));
    validation.validateCustomerName(request,recording.get().customerId);
    String path = getPath(request.getRequestURI(), replayCollection, recordCollection, customerId, app, instanceId, recording.get().id);
    return cubeServerService.fetchGetResponse(request, getBody, path);
  }

  @PostMapping("/mock/{replayCollection}/{recordCollection}/{customerId}/{app}/{instanceId}/{service}/**")
  public ResponseEntity postData(HttpServletRequest request, @RequestBody Optional<String> postBody,
      @PathVariable String replayCollection, @PathVariable String recordCollection,
      @PathVariable String customerId, @PathVariable String app,
      @PathVariable String instanceId, @PathVariable String service) {
    validation.validateCustomerName(request,customerId);
    Optional<Recording> recording = cubeServerService.searchRecording(customerId, app, recordCollection);
    if(recording.isEmpty())
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(String.format("There is no Recording Object for customerId=%s, app=%s, collection=%s",
              customerId, app,  recordCollection));
    validation.validateCustomerName(request,recording.get().customerId);
    String path = getPath(request.getRequestURI(), replayCollection, recordCollection, customerId, app, instanceId, recording.get().id);
    return cubeServerService.fetchPostResponse(request, postBody, path);
  }

  private String getPath(String uri, String replayCollection, String recordCollection,
      String customerId, String app, String instanceId,String recordingId) {
    return uri.replace(String.format("/api/msc/mock/%s/%s/%s/%s/%s",
        replayCollection, recordCollection, customerId, app, instanceId),
        String.format("/ms/mockWithCollection/%s/%s", replayCollection, recordingId));
  }
}
