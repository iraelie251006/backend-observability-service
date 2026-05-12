package tech.iraelie.practice.order.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tech.iraelie.practice.order.dto.OrderCreateRequest;
import tech.iraelie.practice.order.dto.OrderRequest;
import tech.iraelie.practice.order.dto.StatusRequest;

import java.util.List;
import java.util.Optional;

public interface OrderInterface {
    OrderRequest createOrder(OrderCreateRequest order);

    Page<OrderRequest> getAllOrders(Pageable pageable);

    OrderRequest getOrderById(String id);

    OrderRequest updateOrderById(String id, OrderRequest order);

    OrderRequest updatePartialOrderData(String id, OrderRequest order);

    OrderRequest updateStatusById(String id, StatusRequest status);

    void deleteOrderById(String id);
}
