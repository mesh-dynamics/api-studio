package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.PersonalEmailDomains;
import com.cubeui.backend.repository.PersonalEmailDomainsRepository;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/addDomains")
@Secured("ROLE_ADMIN")
public class PersonalEmailDomainsController {
  @Autowired
  private PersonalEmailDomainsRepository personalEmailDomainsRepository;

  @PostMapping("/save")
  public ResponseEntity save(@RequestBody List<String> domains, HttpServletRequest request) {
    final List<PersonalEmailDomains> savedList = new ArrayList<>();
    for(String domain: domains) {
      if(this.personalEmailDomainsRepository.findByDomain(domain).isEmpty()) {
        PersonalEmailDomains saved = this.personalEmailDomainsRepository
            .save(PersonalEmailDomains.builder().domain(domain).build());
        savedList.add(saved);
        log.info(String.format("Domain=%s is added", domain));
      }
    }
    return ResponseEntity.ok(savedList);
  }

}
