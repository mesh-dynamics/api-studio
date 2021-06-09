package com.cubeui.backend.repository;

import com.cubeui.backend.domain.ServiceGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(path = "service-groups", collectionResourceRel = "service-groups", itemResourceRel = "service-group")
public interface ServiceGroupRepository extends JpaRepository<ServiceGroup, Long> {
    Optional<ServiceGroup> findByNameAndAppId(String name, Long aapId);
    Optional<List<ServiceGroup>> findByAppId(Long appId);
}