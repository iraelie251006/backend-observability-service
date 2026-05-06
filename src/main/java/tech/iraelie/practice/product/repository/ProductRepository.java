package tech.iraelie.practice.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.iraelie.practice.product.model.Product;


public interface ProductRepository extends JpaRepository<Product, String> {
}
