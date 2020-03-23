package com.cubeui.backend.repository;

import com.cubeui.backend.domain.JiraCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(path = "Jiradefaultcustomer", collectionResourceRel = "Jiradefaultcustomer", itemResourceRel = "Jiradefaultcustomer")
public interface JiraCustomerRepository extends JpaRepository<JiraCustomer,Long> {
    Optional<JiraCustomer> findByCustomerId(Long id);

}
