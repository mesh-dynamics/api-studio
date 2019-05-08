package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.ProductDTO;
import com.cubeui.backend.domain.Product;
import com.cubeui.backend.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/product")
//@Secured({"ROLE_ADMIN"})
public class ProductController {

    private ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Secured("ROLE_USER")
    @GetMapping("")
    public ResponseEntity all() {
        return ok(this.productRepository.findAll());
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("")
    public ResponseEntity save(@RequestBody ProductDTO productDTO, HttpServletRequest request) {
        Product saved = this.productRepository.save(Product.builder().name(productDTO.getName()).price(productDTO.getPrice()).build());
        return created(
            ServletUriComponentsBuilder
                .fromContextPath(request)
                .path("/api/products/{id}")
                .buildAndExpand(saved.getId())
                .toUri())
            .build();
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.productRepository.findById(id));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<Product> existed = this.productRepository.findById(id);
        this.productRepository.delete(existed.get());
        return noContent().build();
    }
}
