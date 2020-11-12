package com.cubeui.backend.repository;

import com.cubeui.backend.domain.PersonalEmailDomains;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "personal_email_domains", collectionResourceRel = "personal_email_domains", itemResourceRel = "personal_email_domains")
public interface PersonalEmailDomainsRepository extends JpaRepository<PersonalEmailDomains, Long> {
  Optional<PersonalEmailDomains> findByDomain(String domain);
}
