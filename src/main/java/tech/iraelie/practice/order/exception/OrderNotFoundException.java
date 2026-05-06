package tech.iraelie.practice.order.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String id) {
        super(String.format("Order Id: \"%s\" is not found", id));
    }
}
