package com.cubeui.backend.repository;

import com.cubeui.backend.domain.Instance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "instances", collectionResourceRel = "instances", itemResourceRel = "instance")
public interface InstanceRepository extends JpaRepository<Instance, Long> {
    Optional<List<Instance>> findByAppId(Long appId);
    Optional<Instance> findByNameAndAppIdAndGatewayEndpoint(String name, Long appId, String gatewayEndpoint);
}
