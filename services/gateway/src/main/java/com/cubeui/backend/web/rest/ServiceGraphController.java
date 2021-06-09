package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.ServiceGraphDTO;
import com.cubeui.backend.domain.GraphEdge;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.ServiceGraph;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.ServiceGraphRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import io.md.dao.ApiTraceResponse;
import io.md.dao.ApiTraceResponse.ServiceReqRes;
import io.md.utils.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.thymeleaf.util.StringUtils;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/service_graph")
//@Secured({"ROLE_USER"})
public class ServiceGraphController {

    @Autowired
    private AppRepository appRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private ServiceGraphRepository serviceGraphRepository;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private CubeServerService cubeServerService;
    @Autowired
    private Validation validation;

    @PostMapping("")
    public ResponseEntity save(@RequestBody ServiceGraphDTO serviceGraphDTO, HttpServletRequest request) {
        if (serviceGraphDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("ServiceGraph with ID '" + serviceGraphDTO.getId() +"' already exists."));
        }

        Optional<App> app = appRepository.findById(serviceGraphDTO.getAppId());
        Optional<Service> fromService = serviceRepository.findById(serviceGraphDTO.getFromServiceId());
        Optional<Service> toService = serviceRepository.findById(serviceGraphDTO.getToServiceId());
        if (app.isPresent() && fromService.isPresent() && toService.isPresent()) {
            Optional<ServiceGraph> serviceGraph = this.serviceGraphRepository.findByAppIdAndFromServiceIdAndToServiceId(
                    serviceGraphDTO.getAppId(), serviceGraphDTO.getFromServiceId(), serviceGraphDTO.getToServiceId());
            if (serviceGraph.isPresent())
                return ok(serviceGraph);
            ServiceGraph saved = this.serviceGraphRepository.save(
                    ServiceGraph.builder()
                            .app(app.get())
                            .fromService(fromService.get())
                            .toService(toService.get())
                            .build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/service_graph/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordNotFoundException("App with ID '" + serviceGraphDTO.getId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody ServiceGraphDTO serviceGraphDTO, HttpServletRequest request) {
        if (serviceGraphDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("ServiceGraph id not provided"));
        }
        Optional<ServiceGraph> existing = serviceGraphRepository.findById(serviceGraphDTO.getId());
        if (existing.isPresent()) {
            Optional.ofNullable(serviceGraphDTO.getAppId()).ifPresent(appId -> {
                Optional<App> app = Optional.ofNullable(appRepository.findById(appId)).get();
                if (app.isPresent()) {
                    existing.get().setApp(app.get());
                } else {
                    throw new RecordNotFoundException("App with ID '" + serviceGraphDTO.getAppId() + "' not found.");
                }
            });
            Optional.ofNullable(serviceGraphDTO.getFromServiceId()).ifPresent(fromServiceId -> {
                Optional<Service> fromService = Optional.ofNullable(serviceRepository.findById(fromServiceId)).get();
                if (fromService.isPresent()) {
                    existing.get().setFromService(fromService.get());
                } else {
                    throw new RecordNotFoundException("Service with ID '" + serviceGraphDTO.getFromServiceId()+ "' not found.");
                }
            });
            Optional.ofNullable(serviceGraphDTO.getToServiceId()).ifPresent(toServiceId -> {
                Optional<Service> toService = Optional.ofNullable(serviceRepository.findById(toServiceId)).get();
                if (toService.isPresent()) {
                    existing.get().setToService(toService.get());
                } else {
                    throw new RecordNotFoundException("Service with ID '" + serviceGraphDTO.getToServiceId()+ "' not found.");
                }
            });
            this.serviceGraphRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/service_graph/{id}")
                            .buildAndExpand(existing.get().getId())
                            .toUri())
                    .body(existing);
        } else {
            throw new RecordNotFoundException("ServiceGraph with ID '" + serviceGraphDTO.getId() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.serviceGraphRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<ServiceGraph> existed = this.serviceGraphRepository.findById(id);
        this.serviceGraphRepository.delete(existed.get());
        return noContent().build();
    }

    @Transactional
    @PostMapping("/generateServiceGraph/{customerId}/{appId}")
    public  ResponseEntity generateServiceGraph(HttpServletRequest request,
        @PathVariable String customerId, @PathVariable String appId,
        @RequestParam(value="saveGraph", required = false, defaultValue = "false") boolean saveServiceGraph, Authentication authentication) {
        Optional<Customer> existingCustomer = this.customerService.getByName(customerId);
        if(existingCustomer.isEmpty()) {
            return status(BAD_REQUEST).body(new ErrorResponse("customer with Name '" + customerId + "' not found."));
        }
        validation.validateCustomerName(authentication , customerId);
        Optional<App> existingApp = this.appRepository.findByNameAndCustomerId(appId, existingCustomer.get().getId());
        if(existingApp.isEmpty()) {
            return status(BAD_REQUEST).body(new ErrorResponse("App with Name '" + appId + "' not found."));
        }
        Long id = existingApp.get().getId();
        Optional<List<ApiTraceResponse>> response = cubeServerService.getApiTrace(request, customerId, appId);
        List<ServiceGraph> serviceGraphResponse = new ArrayList<>();
        Set<GraphEdge> graph = new HashSet<>();
        if(saveServiceGraph) {
            this.serviceGraphRepository.deleteAllByAppId(id);
        }
        if(response.isPresent()) {
            List<ApiTraceResponse> apiTraceResponses = response.get();
            apiTraceResponses.forEach(apiTraceResponse -> {
                List<ServiceReqRes> resp = apiTraceResponse.res;
                Map<String, ServiceReqRes> serviceReqResMapBySpanId = new HashMap<>();
                resp.forEach(serviceReqRes -> serviceReqResMapBySpanId.put(serviceReqRes.spanId, serviceReqRes));

                resp.forEach(serviceReqRes -> {
                    ServiceReqRes fromServiceReqRes = serviceReqResMapBySpanId.get(serviceReqRes.parentSpanId);
                    if(fromServiceReqRes != null) {
                        graph.add(new GraphEdge(fromServiceReqRes.service, serviceReqRes.service));
                    }
                });
            });
        }
        graph.forEach(graphEdge -> {
            Optional<Service> fromService = this.serviceRepository.findByNameAndAppId(graphEdge.getFrom(), id);
            Optional<Service> toService = this.serviceRepository.findByNameAndAppId(graphEdge.getTo(), id);
            if(fromService.isPresent() && toService.isPresent()) {
                ServiceGraph serviceGraph = ServiceGraph.builder()
                    .app(existingApp.get())
                    .fromService(fromService.get())
                    .toService(toService.get())
                    .build();

                serviceGraphResponse.add(serviceGraph);
                if(saveServiceGraph) {
                    this.serviceGraphRepository.save(serviceGraph);
                }
            }
        });
        return ok(serviceGraphResponse);
    }
}
