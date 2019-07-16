package com.cubeui.backend.repository;

import com.cubeui.backend.domain.ServiceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "service-groups", collectionResourceRel = "service-groups", itemResourceRel = "service-group")
public interface ServiceGroupRepository extends JpaRepository<ServiceGroup, Long> {
}