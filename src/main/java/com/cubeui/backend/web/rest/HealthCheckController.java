package com.cubeui.backend.web.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    public HealthCheckController() {

    }

    @GetMapping("")
    public ResponseEntity all() {
        return ResponseEntity.ok("All Is Well!");
    }
}
