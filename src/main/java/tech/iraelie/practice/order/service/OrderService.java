package tech.iraelie.practice.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.iraelie.practice.order.dto.OrderCreateRequest;
import tech.iraelie.practice.order.dto.OrderRequest;
import tech.iraelie.practice.order.dto.StatusRequest;
import tech.iraelie.practice.order.exception.OrderNotFoundException;
import tech.iraelie.practice.order.model.Order;
import tech.iraelie.practice.order.repository.OrderRepository;
import tech.iraelie.practice.user.model.User;
import tech.iraelie.practice.user.dto.UserDTO;
import tech.iraelie.practice.user.exception.UserNotFoundException;
import tech.iraelie.practice.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService implements OrderInterface {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public OrderRequest createOrder(OrderCreateRequest order) {
        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new UserNotFoundException(order.getUserId()));

        Order newOrder = Order.builder()
                .user(user)
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .build();

        orderRepository.save(newOrder);

        return mapToOrderRequest(newOrder);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderRequest> getAllOrders() {
        return orderRepository.findAll()
                .stream().map(this::mapToOrderRequest).toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public Optional<OrderRequest> getOrderById(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return Optional.of(mapToOrderRequest(order));
    }

    @Override
    @Transactional
    @PreAuthorize("#id == authentication.principal.id")
    public Optional<OrderRequest> updateOrderById(String id, OrderRequest orderRequest) {
        User user = userRepository.findById(orderRequest.getUser().getId())
                .orElseThrow(() -> new UserNotFoundException(orderRequest.getUser().getId()));

        return Optional.ofNullable(orderRepository.findById(id)
                .map(this::mapToOrderRequest)
                .orElseThrow(() -> new OrderNotFoundException(id))
        );
    }

    @Override
    @Transactional
    public Optional<OrderRequest> updatePartialOrderData(String id, OrderRequest orderRequest) {
        return Optional.ofNullable(orderRepository.findById(id)
                .map(order -> {
                    if (orderRequest.getTotalAmount() != null) {
                        order.setTotalAmount(orderRequest.getTotalAmount());
                    }
                    if (orderRequest.getOrderStatus() != null) {
                        order.setOrderStatus(orderRequest.getOrderStatus());
                    }
                    return mapToOrderRequest(order);
                }).orElseThrow(() -> new OrderNotFoundException(id)));
    }

    @Override
    @Transactional
    public Optional<OrderRequest> updateStatusById(String id, StatusRequest status) {
        return Optional.ofNullable(orderRepository.findById(id)
                .map(order -> {
                    if (status.getStatus() != order.getOrderStatus()) {
                        order.setOrderStatus(status.getStatus());
                    }
                    return mapToOrderRequest(order);
                }).orElseThrow(() -> new OrderNotFoundException(id)));
    }

    @Override
    public boolean deleteOrderById(String id) {
        return orderRepository.findById(id)
                .map(order -> {
                    orderRepository.deleteById(id);
                    return true;
                })
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public OrderRequest mapToOrderRequest(Order order) {
        return OrderRequest.builder()
                .id(order.getId())
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .user(
                        UserDTO.builder()
                                .id(order.getUser().getId())
                                .email(order.getUser().getEmail())
                                .name(order.getUser().getName())
                                .createdAt(order.getUser().getCreatedAt())
                                .build()
                )
                .build();
    }
}
