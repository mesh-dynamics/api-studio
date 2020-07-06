package com.cubeui.backend.web.external;

import com.cubeui.backend.domain.DTO.LogStoreDTO;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.ElasticSearchService;
import javax.servlet.http.HttpServletRequest;
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

  @Autowired
  private ElasticSearchService elasticSearchService;

  @Autowired
  private Validation validation;

  @PostMapping("/{customerId}")
  public ResponseEntity postLogData(HttpServletRequest request, @RequestBody LogStoreDTO postBody,
      @PathVariable String customerId) {

    validation.validateCustomerName(request, customerId);
    return elasticSearchService.postLogData(customerId, postBody);
  }
}
