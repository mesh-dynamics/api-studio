package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.*;
import com.cubeui.backend.domain.DTO.InstanceDTO;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.InstanceRepository;
import com.cubeui.backend.repository.InstanceUserRepository;
import com.cubeui.backend.repository.UserRepository;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/instance")
public class InstanceController {

    private InstanceRepository instanceRepository;
    private AppRepository appRepository;
    private InstanceUserRepository instanceUserRepository;
    private UserRepository userRepository;

    public InstanceController(InstanceRepository instanceRepository, AppRepository appRepository, InstanceUserRepository instanceUserRepository,
        UserRepository userRepository) {
        this.instanceRepository = instanceRepository;
        this.appRepository = appRepository;
        this.instanceUserRepository = instanceUserRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("")
    public ResponseEntity all(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<List<App>> appList = appRepository.findByCustomerId(user.getCustomer().getId());
        Optional<List<InstanceUser>> instanceUserList = instanceUserRepository.findByUserId(user.getId());
        if(appList.isPresent() && instanceUserList.isPresent()) {
            List<Instance> instancesList = new ArrayList<Instance>();
            for( App app : appList.get()) {
                Optional<List<Instance>> optionalInstances = this.instanceRepository.findByAppId(app.getId());
                optionalInstances.ifPresent(instances -> {
                    instances.forEach(instance -> {
                        instanceUserList.get().forEach(instanceUser -> {
                            if(instanceUser.getInstance().getId().equals(instance.getId())) instancesList.add(instance);
                        });
                    });
                });
            }
            return ok(Optional.ofNullable(instancesList));
        }
        return status(NOT_FOUND).body(new ErrorResponse("Instances for user name, '" + user.getName() + "' not found."));
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody InstanceDTO instanceDTO, HttpServletRequest request) {
        if (instanceDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("Instance with ID '" + instanceDTO.getId() +"' already exists."));
        }
        Optional<App> app = appRepository.findById(instanceDTO.getAppId());
        if(app.isPresent() && StringUtils.isNoneBlank(instanceDTO.getName())) {
            Optional<Instance> instance = this.instanceRepository.findByNameAndAppId(
                        instanceDTO.getName(), instanceDTO.getAppId());
            if (instance.isPresent()) {
                return ok(instance);
            }
            Instance saved = this.instanceRepository.save(Instance.builder()
                    .name(instanceDTO.getName())
                    .app(app.get())
                    .gatewayEndpoint(instanceDTO.getGatewayEndpoint())
                    .loggingURL(instanceDTO.getLoggingURL())
                    .build());
            Optional<List<User>> optionalUsers = this.userRepository.findByCustomerId(app.get().getCustomer().getId());
            optionalUsers.ifPresent(users -> {
                users.forEach(user -> {
                    InstanceUser instanceUser = new InstanceUser();
                    instanceUser.setUser(user);
                    instanceUser.setInstance(saved);
                    this.instanceUserRepository.save(instanceUser);
                });
            });
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/instance/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordNotFoundException("App with ID '" + instanceDTO.getAppId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody InstanceDTO instanceDTO, HttpServletRequest request) {
        Optional<Instance> existing = this.instanceRepository.findById(instanceDTO.getId());
        if (existing.isPresent()) {
            existing.ifPresent(instance -> {
                instance.setName(instanceDTO.getName());
                instance.setApp(appRepository.findById(instanceDTO.getAppId()).get());
                instance.setGatewayEndpoint(instanceDTO.getGatewayEndpoint());
                instance.setLoggingURL(instanceDTO.getLoggingURL());
                this.instanceRepository.save(instance);
            });
            this.instanceRepository.save(existing.get());
            return created(
                ServletUriComponentsBuilder
                        .fromContextPath(request)
                        .path("/api/instance/{id}")
                        .buildAndExpand(existing.get().getId())
                        .toUri())
                .body("Instance with ID '" + existing.get().getId() + "' updated");
        } else {
            throw new RecordNotFoundException("Instance with ID '" + instanceDTO.getId() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.instanceRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<Instance> existed = this.instanceRepository.findById(id);
        this.instanceRepository.delete(existed.get());
        return ok().body("Instance with ID '" + id + "' removed successfully");
    }
}
