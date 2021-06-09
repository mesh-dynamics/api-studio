package com.cubeui.backend.repository;

import com.cubeui.backend.domain.TestConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "tests", collectionResourceRel = "tests", itemResourceRel = "testConfig")
public interface TestConfigRepository extends JpaRepository<TestConfig, Long> {
    Optional<List<TestConfig>> findByAppId(Long appId);
    Optional<TestConfig> findByTestConfigNameAndAppId(String testConfigName, Long appId);
}
