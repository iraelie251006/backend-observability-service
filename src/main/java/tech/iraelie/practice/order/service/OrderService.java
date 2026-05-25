package tech.iraelie.practice.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.iraelie.practice.order.dto.OrderCreateRequest;
import tech.iraelie.practice.order.dto.OrderRequest;
import tech.iraelie.practice.order.dto.StatusRequest;
import tech.iraelie.practice.order.exception.OrderNotFoundException;
import tech.iraelie.practice.order.model.Order;
import tech.iraelie.practice.order.repository.OrderRepository;
import tech.iraelie.practice.user.dto.UserDTO;
import tech.iraelie.practice.user.exception.UserNotFoundException;
import tech.iraelie.practice.user.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService implements OrderInterface {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @CachePut(value = "order", key = "#return.id")
    @CacheEvict(value = "ordersPage", allEntries = true)
    public OrderRequest createOrder(OrderCreateRequest request) {
        log.info("Creating order userId={}", request.getUserId());

        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> {
                    log.warn("User not found during order creation userId={}", request.getUserId());
                    return new UserNotFoundException(request.getUserId());
                });

        Order order = Order.builder()
                .user(user)
                .orderStatus(request.getOrderStatus())
                .totalAmount(request.getTotalAmount())
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order created orderId={} userId={}", saved.getId(), request.getUserId());
        return mapToOrderRequest(saved);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "ordersPage", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<OrderRequest> getAllOrders(Pageable pageable) {
        log.info("Fetching all orders page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<OrderRequest> page = orderRepository.findAll(pageable).map(this::mapToOrderRequest);
        log.info("Fetched orders totalElements={}", page.getTotalElements());
        return page;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @Cacheable(value = "order", key = "#id")
    public OrderRequest getOrderById(String id) {
        log.info("Fetching order orderId={}", id);
        return orderRepository.findById(id)
                .map(this::mapToOrderRequest)
                .orElseThrow(() -> {
                    log.warn("Order not found orderId={}", id);
                    return new OrderNotFoundException(id);
                });
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @CachePut(value = "order", key = "#id")
    @CacheEvict(value = "ordersPage", key = "#id", allEntries = true)
    public OrderRequest updateOrderById(String id, OrderRequest orderRequest) {
        log.info("Updating order orderId={}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order not found for update orderId={}", id);
                    return new OrderNotFoundException(id);
                });

        if (orderRequest.getTotalAmount() != null) {
            order.setTotalAmount(orderRequest.getTotalAmount());
        }
        if (orderRequest.getOrderStatus() != null) {
            order.setOrderStatus(orderRequest.getOrderStatus());
        }
        // User reassignment: validate new user exists before binding
        if (orderRequest.getUser() != null && orderRequest.getUser().getId() != null) {
            var user = userRepository.findById(orderRequest.getUser().getId())
                    .orElseThrow(() -> {
                        log.warn("User not found during order update userId={}", orderRequest.getUser().getId());
                        return new UserNotFoundException(orderRequest.getUser().getId());
                    });
            order.setUser(user);
        }

        log.info("Order updated orderId={}", id);
        return mapToOrderRequest(order);
    }

    @Override
    @Transactional
    @CachePut(value = "order", key = "#id")
    @CacheEvict(value = "ordersPage", key = "#id", allEntries = true)
    public OrderRequest updatePartialOrderData(String id, OrderRequest orderRequest) {
        log.info("Partial update for orderId={}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order not found for partial update orderId={}", id);
                    return new OrderNotFoundException(id);
                });

        if (orderRequest.getTotalAmount() != null) {
            order.setTotalAmount(orderRequest.getTotalAmount());
        }
        if (orderRequest.getOrderStatus() != null) {
            order.setOrderStatus(orderRequest.getOrderStatus());
        }

        log.info("Partial update complete orderId={}", id);
        return mapToOrderRequest(order);
    }

    @Override
    @Transactional
    @CachePut(value = "order", key = "#id")
    @CacheEvict(value = "ordersPage", key = "#id", allEntries = true)
    public OrderRequest updateStatusById(String id, StatusRequest status) {
        log.info("Updating status for orderId={}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order not found for status update orderId={}", id);
                    return new OrderNotFoundException(id);
                });

        if (!status.getStatus().equals(order.getOrderStatus())) {
            log.info("Status transition orderId={} from={} to={}", id, order.getOrderStatus(), status.getStatus());
            order.setOrderStatus(status.getStatus());
        } else {
            log.debug("Status unchanged orderId={} status={}", id, status.getStatus());
        }

        return mapToOrderRequest(order);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"order", "ordersPage"}, key = "#id", allEntries = true)
    public void deleteOrderById(String id) {
        log.info("Deleting order orderId={}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order not found for deletion orderId={}", id);
                    return new OrderNotFoundException(id);
                });

        orderRepository.delete(order);
        log.info("Order deleted orderId={}", id);
    }

    private OrderRequest mapToOrderRequest(Order order) {
        return OrderRequest.builder()
                .id(order.getId())
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .user(UserDTO.builder()
                        .id(order.getUser().getId())
                        .email(order.getUser().getEmail())
                        .name(order.getUser().getName())
                        .createdAt(order.getUser().getCreatedAt())
                        .build())
                .build();
    }
}