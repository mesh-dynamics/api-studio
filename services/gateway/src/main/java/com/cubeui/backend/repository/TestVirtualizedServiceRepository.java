package com.cubeui.backend.repository;

import com.cubeui.backend.domain.TestVirtualizedService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "test_virtualized_services", collectionResourceRel = "test_virtualized_services", itemResourceRel = "test_virtualized_service")
public interface TestVirtualizedServiceRepository extends JpaRepository<TestVirtualizedService, Long> {
    Optional<List<TestVirtualizedService>> findByTestConfigId(Long testConfigId);
    Optional<TestVirtualizedService> findByTestConfigIdAndServiceId(Long testId, Long serviceId);
}
