package com.cubeui.backend.repository;

import com.cubeui.backend.domain.TestVirtualizedService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "test_virtualized_services", collectionResourceRel = "test_virtualized_services", itemResourceRel = "test_virtualized_service")
public interface TestVirtualizedServiceRepository extends JpaRepository<TestVirtualizedService, Long> {
}
