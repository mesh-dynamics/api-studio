package com.cubeui.backend.repository;

import com.cubeui.backend.domain.JiraUserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "jirausercredentials", collectionResourceRel = "jirausercredentials", itemResourceRel = "jirausercredential")
public interface JiraUserCredentialsRepository extends JpaRepository<JiraUserCredentials, Long> {

  Optional<JiraUserCredentials> findByUserId(Long userId);

}
