package com.cubeui.backend.repository;

import com.cubeui.backend.domain.EmailDomain;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "email_domains", collectionResourceRel = "email_domains", itemResourceRel = "email_domains")
public interface EmailDomainRepository extends JpaRepository<EmailDomain, Long> {
    Optional<EmailDomain> findByDomain(String domain);
    Optional<EmailDomain> findByCustomerId(Long id);
}
