package com.cubeui.backend.repository;

import com.cubeui.backend.domain.Instance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "instances", collectionResourceRel = "instances", itemResourceRel = "instance")
public interface InstanceRepository extends JpaRepository<Instance, Long> {
}
