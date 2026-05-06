package tech.iraelie.practice.product.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String id) {
        super(String.format("Product Id %s is not found", id));
    }
}
