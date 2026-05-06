package tech.iraelie.practice.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.iraelie.practice.product.model.Product;
import tech.iraelie.practice.product.exception.ProductNotFoundException;
import tech.iraelie.practice.product.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService implements ProductInterface{
    private final ProductRepository productRepository;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Optional<Product> update(String id, Product product) {
        return Optional.ofNullable(productRepository.findById(id)
                .map(p -> {
                    p.setName(product.getName());
                    p.setPrice(product.getPrice());
                    return p;
                })
                .orElseThrow(() -> new ProductNotFoundException(id)));
    }

    @Override
    @Transactional
    public boolean deleteById(String id) {
        return productRepository.findById(id)
                .map(p -> {
                    productRepository.deleteById(id);
                    return true;
                })
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
