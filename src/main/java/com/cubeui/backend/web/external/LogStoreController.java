package com.cubeui.backend.web.external;

import com.cubeui.backend.domain.DTO.LogStoreDTO;
import com.cubeui.backend.security.Validation;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logStore")
public class LogStoreController {

  private static Logger LOGGER = LogManager.getLogger(LogStoreController.class);

  @Autowired
  private Validation validation;

  @PostMapping("/{customerId}")
  public ResponseEntity postLogData(HttpServletRequest request, @RequestBody LogStoreDTO postBody,
      @PathVariable String customerId) {

    validation.validateCustomerName(request, customerId);
    LOGGER.error(new ObjectMessage(Map.of("customerId", customerId,
        "app", postBody.app, "instance", postBody.instance, "service", postBody.service,
        "version", postBody.version, "sourceType", postBody.sourceType,
        "logMessage", postBody.logMessage, "level", postBody.level)));
    return ResponseEntity.ok("Data added");
  }
}
