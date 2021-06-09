package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.TestPathDTO;
import com.cubeui.backend.domain.Path;
import com.cubeui.backend.domain.TestConfig;
import com.cubeui.backend.domain.TestPath;
import com.cubeui.backend.repository.PathRepository;
import com.cubeui.backend.repository.TestConfigRepository;
import com.cubeui.backend.repository.TestPathRepository;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.ResponseEntity.*;
import static org.springframework.http.ResponseEntity.noContent;

@RestController
@RequestMapping("/api/test-path")
public class TestPathController {

    private TestConfigRepository testConfigRepository;
    private PathRepository pathRepository;
    private TestPathRepository testPathRepository;

    public TestPathController(TestConfigRepository testConfigRepository, PathRepository pathRepository, TestPathRepository testPathRepository) {
        this.testConfigRepository = testConfigRepository;
        this.pathRepository = pathRepository;
        this.testPathRepository = testPathRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        if(id == null) {
            return status(BAD_REQUEST).body(new ErrorResponse("TestPath with ID '" + id +"' is not present."));
        }
        Optional<TestPath> existed = this.testPathRepository.findById(id);
        if(existed.isPresent()) {
            return ok(this.testPathRepository.findById(id));
        } else {
            return status(NOT_FOUND).body(new ErrorResponse("TestPath with ID '" + id + "' not found."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        if(id == null) {
            return status(BAD_REQUEST).body(new ErrorResponse("TestPath with ID '" + id +"' is not present."));
        }
        Optional<TestPath> existed = this.testPathRepository.findById(id);
        if(existed.isPresent()) {
            this.testPathRepository.delete(existed.get());
            return noContent().build();
        } else {
            return status(NOT_FOUND).body(new ErrorResponse("TestPath with ID '" + id + "' not found."));
        }
    }

    @PostMapping("")
    public ResponseEntity save (@RequestBody TestPathDTO testPathDTO, HttpServletRequest request) {
        if(testPathDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("TestPath with ID '" + testPathDTO.getId() +"' already exists."));
        }
        Optional<TestConfig> testConfig = null;
        if(testPathDTO.getTestId() != null) {
            testConfig = testConfigRepository.findById(testPathDTO.getTestId());
            if(testConfig.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("TestConfig with ID '" + testPathDTO.getTestId() + "' not found."));
        } else {
            return status(BAD_REQUEST).body(new ErrorResponse("Mandatory field TestConfig Id is empty."));
        }
        Optional<Path> path = null;
        if(testPathDTO.getPathId() != null) {
            path = pathRepository.findById(testPathDTO.getPathId());
            if(path.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("Path with ID '" + testPathDTO.getPathId() + "' not found."));
        } else {
            return status(BAD_REQUEST).body(new ErrorResponse("Mandatory field Path Id is empty."));
        }
        Optional<TestPath> testPath = this.testPathRepository.findByTestConfigIdAndPathId(testPathDTO.getTestId(), testPathDTO.getPathId());
        if (testPath.isPresent()) {
            return ok(testPath);
        }
        TestPath saved = this.testPathRepository.save(
                TestPath.builder()
                .testConfig(testConfig.get())
                .path(path.get())
                .build());
        return created(
                ServletUriComponentsBuilder
                    .fromContextPath(request)
                    .path(request.getServletPath() + "/{id")
                    .buildAndExpand(saved.getId())
                    .toUri())
                .body(saved);
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody TestPathDTO testPathDTO, HttpServletRequest request) {
        if(testPathDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("Mandatory field TestPath id is not present in the request."));
        }
        Optional<TestPath> existing = testPathRepository.findById(testPathDTO.getId());
        if(existing.isPresent()) {
            Optional.ofNullable(testPathDTO.getTestId()).ifPresent(testConfigId -> {
                Optional<TestConfig> testConfig = Optional.ofNullable(testConfigRepository.findById(testConfigId)).get();
                if(testConfig.isPresent()) {
                    existing.get().setTestConfig(testConfig.get());
                } else {
                    throw new RecordNotFoundException("TestConfig with ID '" + testPathDTO.getTestId() + "' not found.");
                }
            });
            Optional.ofNullable(testPathDTO.getPathId()).ifPresent(pathId -> {
                Optional<Path> path = Optional.ofNullable(pathRepository.findById(pathId)).get();
                if(path.isPresent()) {
                    existing.get().setPath(path.get());
                } else {
                    throw new RecordNotFoundException("Path with ID '" + testPathDTO.getPathId() + "' not found.");
                }
            });
            this.testPathRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path(request.getServletPath() + "/{id")
                            .buildAndExpand(existing.get().getId())
                            .toUri())
                    .body(existing);
        } else {
            return status(NOT_FOUND).body(new ErrorResponse("TestPath with ID '" + testPathDTO.getId() + "' not found."));
        }
    }
}
