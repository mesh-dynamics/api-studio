package com.cubeui.backend.repository;

import com.cubeui.backend.domain.Path;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(path = "paths", collectionResourceRel = "paths", itemResourceRel = "path")
public interface PathRepository extends JpaRepository<Path, Long> {
    Optional<Path> findByPathAndServiceId(String path, Long serviceId);
}
