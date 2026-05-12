package tech.iraelie.practice.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tech.iraelie.practice.product.model.Product;

public interface ProductInterface {
    Page<Product> getAllProducts(Pageable pageable);

    Product getProductById(String id);

    Product save(Product product);

    Product update(String id, Product product);

    void deleteById(String id);
}
