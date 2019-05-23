package com.cubeui.backend.repository;

import com.cubeui.backend.domain.TestConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "tests", collectionResourceRel = "tests", itemResourceRel = "testConfig")
public interface TestConfigRepository extends JpaRepository<TestConfig, Long> {
}
