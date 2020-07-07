package com.cubeui.backend.web.external;

import com.cubeui.backend.domain.DTO.LogStoreDTO;
import com.cubeui.backend.security.Validation;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logStore")
@Slf4j
public class LogStoreController {

  @Autowired
  private Validation validation;

  @PostMapping("/{customerId}")
  public ResponseEntity postLogData(HttpServletRequest request, @RequestBody LogStoreDTO postBody,
      @PathVariable String customerId) {

    validation.validateCustomerName(request, customerId);
    log.error(String.format("customerId=%s, app=%s, instance=%s, service=%s, version=%s, sourceType=%s, logMessage=%s",
        customerId, postBody.app, postBody.instance, postBody.service, postBody.version,
        postBody.sourceType, postBody.logMessage));
    return ResponseEntity.ok("Data added");
  }
}
