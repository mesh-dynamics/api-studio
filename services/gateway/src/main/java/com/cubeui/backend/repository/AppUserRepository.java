package com.cubeui.backend.repository;

import com.cubeui.backend.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "app-users", collectionResourceRel = "app-users", itemResourceRel = "app-user")
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<List<AppUser>> findByUserId(Long userId);

    Optional<List<AppUser>> findByAppId(Long appId);
    Optional<AppUser> findByAppIdAndUserId(Long appId, Long userId);
}
