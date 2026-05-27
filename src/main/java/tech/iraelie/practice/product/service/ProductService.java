package tech.iraelie.practice.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.iraelie.practice.product.model.Product;
import tech.iraelie.practice.product.exception.ProductNotFoundException;
import tech.iraelie.practice.product.repository.ProductRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService implements ProductInterface{
    private final ProductRepository productRepository;

    @Override
    @Cacheable(value = "productsPage", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<Product> getAllProducts(Pageable pageable) {
        log.info("Fetching products page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Product> page = productRepository.findAll(pageable);
        log.info("Fetched products totalElements={}", page.getTotalElements());
        return page;
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public Product getProductById(String id) {
        log.info("Fetching product productId={}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found productId={}", id);
                    return new ProductNotFoundException(id);
                });
    }

    @Override
    @CachePut(value = "products", key = "#result.id")
    @CacheEvict(value = "productsPage", allEntries = true)
    public Product save(Product product) {
        log.info("Creating product name={}", product.getName());
        Product saved = productRepository.save(product);
        log.info("Product created productId={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    @CachePut(value = "products", key = "#id")
    @CacheEvict(value = "productsPage", allEntries = true)
    public Product update(String id, Product product) {
        log.info("Updating product productId={}", id);
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found for update productId={}", id);
                    return new ProductNotFoundException(id);
                });
        existing.setName(product.getName());
        existing.setPrice(product.getPrice());
        log.info("Product updated productId={}", id);

        return existing;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "productsPage"}, allEntries = true)
    public void deleteById(String id) {
        log.info("Deleting product productId={}", id);
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found for deletion productId={}", id);
                    return new ProductNotFoundException(id);
                });
        productRepository.delete(existing);
        log.info("Product deleted productId={}", id);
    }
}
