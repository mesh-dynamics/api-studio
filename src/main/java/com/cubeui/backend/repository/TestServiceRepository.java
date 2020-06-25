package com.cubeui.backend.repository;

import com.cubeui.backend.domain.TestService;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "test_services", collectionResourceRel = "test_services", itemResourceRel = "test_service")
public interface TestServiceRepository extends JpaRepository<TestService, Long> {
  Optional<List<TestService>> findByTestConfigId(Long testConfigId);
  Optional<TestService> findByTestConfigIdAndServiceId(Long testId, Long ServiceId);
}
