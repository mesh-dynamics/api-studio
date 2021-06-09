package com.cubeui.backend.repository;

import com.cubeui.backend.domain.JiraCustomerDefaultCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(path = "jiracustomerdefaultcredentials", collectionResourceRel = "jiracustomerdefaultcredentials", itemResourceRel = "jiracustomerdefaultcredentials")
public interface JiraCustomerCredentialsRepository extends JpaRepository<JiraCustomerDefaultCredentials,Long> {
    Optional<JiraCustomerDefaultCredentials> findByCustomerId(Long id);

}
