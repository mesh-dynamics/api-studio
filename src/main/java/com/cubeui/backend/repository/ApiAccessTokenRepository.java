package com.cubeui.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.cubeui.backend.domain.ApiAccessToken;

@RepositoryRestResource(path = "api-access-tokens", collectionResourceRel = "api-access-tokens", itemResourceRel = "api-access-token")
public interface ApiAccessTokenRepository extends JpaRepository<ApiAccessToken, Long> {
    Optional<List<ApiAccessToken>> findByUserId(Long userId);
}
