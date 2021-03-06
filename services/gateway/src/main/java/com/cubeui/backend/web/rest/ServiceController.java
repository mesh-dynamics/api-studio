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

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.ServiceDTO;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.ServiceGroup;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.ServiceGroupRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/service")
//@Secured({"ROLE_USER"})
public class ServiceController {

    private AppRepository appRepository;
    private ServiceRepository serviceRepository;
    private ServiceGroupRepository serviceGroupRepository;

    public ServiceController(AppRepository appRepository, ServiceRepository serviceRepository, ServiceGroupRepository serviceGroupRepository) {
        this.appRepository = appRepository;
        this.serviceRepository = serviceRepository;
        this.serviceGroupRepository = serviceGroupRepository;
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody ServiceDTO serviceDTO, HttpServletRequest request) {
        if (serviceDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("Service with ID '" + serviceDTO.getId() +"' already exists."));
        }
        Optional<App> app = null;
        if(serviceDTO.getAppId() != null) {
            app = appRepository.findById(serviceDTO.getAppId());
            if(app.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("App with ID '" + serviceDTO.getAppId() + "' not found."));
        }
        Optional<ServiceGroup> serviceGroup = null;
        if(serviceDTO.getServiceGroupId() != null) {
            serviceGroup = serviceGroupRepository.findById(serviceDTO.getServiceGroupId());
            if(serviceGroup.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("ServiceGroup with ID '" + serviceDTO.getServiceGroupId() + "' not found."));
        } else {
            return status(BAD_REQUEST).body(new ErrorResponse("Mandatory field ServiceGroup Id is empty."));
        }
        Optional<Service> service = this.serviceRepository.findByNameAndAppIdAndServiceGroupId(
                    serviceDTO.getName(), serviceDTO.getAppId(), serviceDTO.getServiceGroupId());
        if (service.isPresent()) {
            return ok(service);
        }
        Service saved = this.serviceRepository.save(
                Service.builder()
                        .app(app.get())
                        .serviceGroup(serviceGroup.get())
                        .name(serviceDTO.getName())
                        .build());
        return created(
                ServletUriComponentsBuilder
                        .fromContextPath(request)
                        .path(request.getServletPath() + "/{id}")
                        .buildAndExpand(saved.getId())
                        .toUri())
                .body(saved);
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody ServiceDTO serviceDTO, HttpServletRequest request) {
        if (serviceDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("Service id not provided"));
        }
        Optional<Service> existing = serviceRepository.findById(serviceDTO.getId());
        Optional<App> app = null;
        if(serviceDTO.getAppId() != null) {
            app = appRepository.findById(serviceDTO.getAppId());
            if(app.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("App with ID '" + serviceDTO.getAppId() + "' not found."));
        }
        Optional<ServiceGroup> serviceGroup = null;
        if(serviceDTO.getServiceGroupId() != null) {
            serviceGroup = serviceGroupRepository.findById(serviceDTO.getServiceGroupId());
            if(serviceGroup.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("ServiceGroup with ID '" + serviceDTO.getServiceGroupId() + "' not found."));
        }
        if (existing.isPresent()) {
            existing.get().setApp(app.get());
            existing.get().setServiceGroup(serviceGroup.get());
            Optional.ofNullable(serviceDTO.getName()).ifPresent(updatedName -> {
                existing.get().setName(updatedName);
            });
            this.serviceRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path(request.getServletPath() + "/{id}")
                            .buildAndExpand(existing.get().getId())
                            .toUri())
                    .body(existing);
        } else {
            return status(BAD_REQUEST).body(new ErrorResponse("Service with ID '" + serviceDTO.getId() + "' not found."));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.serviceRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<Service> existed = this.serviceRepository.findById(id);
        this.serviceRepository.delete(existed.get());
        return noContent().build();
    }
}
