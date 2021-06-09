package com.cubeui.backend.repository;

import com.cubeui.backend.domain.TestIntermediateService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "test_intermediate_services", collectionResourceRel = "test_intermediate_services", itemResourceRel = "test_intermediate_service")
public interface TestIntermediateServiceRepository extends JpaRepository<TestIntermediateService, Long> {
    Optional<List<TestIntermediateService>> findByTestConfigId(Long testConfigId);
    Optional<TestIntermediateService> findByTestConfigIdAndServiceId(Long testId, Long serviceId);
}
