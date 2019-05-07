package com.cubeui.backend.repository;

import com.cubeui.backend.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "products", collectionResourceRel = "products", itemResourceRel = "product")
public interface ProductRepository extends JpaRepository<Product, Long> {

}
