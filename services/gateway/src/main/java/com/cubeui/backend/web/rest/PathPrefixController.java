/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cubeui.backend.web.rest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import com.cubeui.backend.domain.DTO.PathPrefixDTO;
import com.cubeui.backend.domain.PathPrefix;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.repository.PathPrefixRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/path-prefix")
public class PathPrefixController {
  @Autowired
  private PathPrefixRepository pathPrefixRepository;
  @Autowired
  private ServiceRepository serviceRepository;

  @PostMapping("")
  public ResponseEntity save(@Valid @RequestBody PathPrefixDTO pathPrefixDTO, HttpServletRequest request) {
    Optional<Service> service =  null;
    if(pathPrefixDTO.getServiceId() != null) {
      service = serviceRepository.findById(pathPrefixDTO.getServiceId());
      if (service.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("Service with ID '" + pathPrefixDTO.getServiceId() + "' not found."));
    } else {
      return status(BAD_REQUEST).body(new ErrorResponse("Mandatory field Service Id is empty."));
    }
    Service serviceValue = service.get();
    List<PathPrefix> saved = new ArrayList<>();
    pathPrefixDTO.getPrefixes().forEach(prefix -> {
      Optional<PathPrefix> existing = pathPrefixRepository.findByPrefixAndServiceId(prefix, pathPrefixDTO.getServiceId());
      existing.ifPresentOrElse(ex ->  {
        ex.setPrefix(prefix);
        saved.add(pathPrefixRepository.save(ex));
        }, () ->
        saved.add(this.pathPrefixRepository.save
            (PathPrefix.builder().
                prefix(prefix)
                .service(serviceValue).build())));
    });

    return created(
        ServletUriComponentsBuilder
            .fromContextPath(request)
            .path(request.getServletPath())
            .buildAndExpand()
            .toUri())
        .body(saved);
  }

  @GetMapping("/{serviceId}")
  public ResponseEntity getPathPrefixesByServiceId(HttpServletRequest request, @PathVariable Long serviceId) {
    Optional<Service> service = this.serviceRepository.findById(serviceId);
    return service.map(s -> ok(pathPrefixRepository.findByServiceId(s.getId()))).orElseThrow(() -> new RecordNotFoundException("No service found for given Id"));
  }

}
