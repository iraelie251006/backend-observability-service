package tech.iraelie.practice.product.service;

import tech.iraelie.practice.product.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductInterface {
    List<Product> getAllProducts();

    Optional<Product> getProductById(String id);

    Product save(Product product);

    Optional<Product> update(String id, Product product);

    boolean deleteById(String id);
}
