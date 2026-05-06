package tech.iraelie.practice.order.service;

import tech.iraelie.practice.order.dto.OrderCreateRequest;
import tech.iraelie.practice.order.dto.OrderRequest;
import tech.iraelie.practice.order.dto.StatusRequest;

import java.util.List;
import java.util.Optional;

public interface OrderInterface {
    OrderRequest createOrder(OrderCreateRequest order);

    List<OrderRequest> getAllOrders();

    Optional<OrderRequest> getOrderById(String id);

    Optional<OrderRequest> updateOrderById(String id, OrderRequest order);

    Optional<OrderRequest> updatePartialOrderData(String id, OrderRequest order);

    Optional<OrderRequest> updateStatusById(String id, StatusRequest status);

    boolean deleteOrderById(String id);
}
