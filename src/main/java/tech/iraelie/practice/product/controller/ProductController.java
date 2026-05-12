package tech.iraelie.practice.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.iraelie.practice.product.model.Product;
import tech.iraelie.practice.product.service.ProductInterface;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductInterface productService;

    @GetMapping("/all")
    public ResponseEntity<Page<Product>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction
    ) {
        MDC.put("endpoint", "GET /api/products/all");

        try {
            log.info("Request received page={} size={} sortBy={} direction={}", page, size, sortBy, direction);
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<Product> products = productService.getAllProducts(pageable);
            log.info("Request completed totalElements={}", products.getTotalElements());
            return ResponseEntity.ok(products);
        } finally {
            MDC.remove("endpoint");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        MDC.put("endpoint", "GET /api/products/{id}");
        MDC.put("productId", id);
        try {
            log.info("Request received");
            Product product = productService.getProductById(id);
            log.info("Request completed");
            return ResponseEntity.ok(product);
        } finally {
            MDC.remove("productId");
            MDC.remove("endpoint");
        }
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        MDC.put("endpoint", "POST /api/products");
        try {
            log.info("Request received name={}", product.getName());
            Product created = productService.save(product);
            log.info("Request completed productId={}", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } finally {
            MDC.remove("endpoint");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody Product product) {

        MDC.put("endpoint", "PUT /api/products/{id}");
        MDC.put("productId", id);
        try {
            log.info("Request received");
            Product updated = productService.update(id, product);
            log.info("Request completed");
            return ResponseEntity.ok(updated);
        } finally {
            MDC.remove("productId");
            MDC.remove("endpoint");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        MDC.put("endpoint", "DELETE /api/products/{id}");
        MDC.put("productId", id);
        try {
            log.info("Request received");
            productService.deleteById(id);
            log.info("Request completed");
            return ResponseEntity.noContent().build();
        } finally {
            MDC.remove("productId");
            MDC.remove("endpoint");
        }
    }
}
