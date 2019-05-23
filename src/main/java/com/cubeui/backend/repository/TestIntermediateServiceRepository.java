package com.cubeui.backend.repository;

import com.cubeui.backend.domain.TestIntermediateService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "test_intermediate_services", collectionResourceRel = "test_intermediate_services", itemResourceRel = "test_intermediate_service")
public interface TestIntermediateServiceRepository extends JpaRepository<TestIntermediateService, Long> {
}
