package com.cubeui.backend.repository;

import com.cubeui.backend.domain.App;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "apps", collectionResourceRel = "apps", itemResourceRel = "app")
public interface AppRepository extends JpaRepository<App, Long> {
    Optional<List<App>> findByCustomerId(Long customerId);
    Optional<App> findByNameAndCustomerId(String name, Long customerId);
}
