package com.cubeui.backend.repository;

import com.cubeui.backend.domain.ServiceGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "service_graphs", collectionResourceRel = "service_graphs", itemResourceRel = "service_graph")
public interface ServiceGraphRepository extends JpaRepository<ServiceGraph, Long> {
}
