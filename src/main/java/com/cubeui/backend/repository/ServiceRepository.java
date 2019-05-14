package com.cubeui.backend.repository;

import com.cubeui.backend.domain.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "services", collectionResourceRel = "services", itemResourceRel = "service")
public interface ServiceRepository extends JpaRepository<Service, Long> {
}
