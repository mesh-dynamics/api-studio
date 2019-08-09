package com.cubeui.backend.repository;

import com.cubeui.backend.domain.TestPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "test_paths", collectionResourceRel = "test_paths", itemResourceRel = "test_path")
public interface TestPathRepository extends JpaRepository<TestPath, Long> {
}
