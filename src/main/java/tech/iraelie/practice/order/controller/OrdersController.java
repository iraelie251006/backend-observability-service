package tech.iraelie.practice.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.iraelie.practice.order.dto.OrderCreateRequest;
import tech.iraelie.practice.order.dto.OrderRequest;
import tech.iraelie.practice.order.dto.StatusRequest;
import tech.iraelie.practice.order.service.OrderInterface;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrderInterface orderService;

    @PostMapping
    public ResponseEntity<OrderRequest> createOrder(@Valid @RequestBody OrderCreateRequest order) {
        MDC.put("endpoint", "POST /api/orders");
        try {
            log.info("Request received userId={}", order.getUserId());
            OrderRequest created = orderService.createOrder(order);
            log.info("Request completed orderId={}", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } finally {
            MDC.remove("endpoint");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderRequest> getOrderById(@PathVariable String id) {
        MDC.put("endpoint", "GET /api/orders/{id}");
        MDC.put("orderId", id);
        try {
            log.info("Request received");
            OrderRequest order = orderService.getOrderById(id);
            log.info("Request completed");
            return ResponseEntity.ok(order);
        } finally {
            MDC.remove("orderId");
            MDC.remove("endpoint");
        }
    }

    @GetMapping("/all")
    public ResponseEntity<Page<OrderRequest>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        MDC.put("endpoint", "GET /api/orders/all");
        try {
            log.info("Request received page={} size={} sortBy={} direction={}", page, size, sortBy, direction);
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<OrderRequest> orders = orderService.getAllOrders(pageable);
            log.info("Request completed totalElements={}", orders.getTotalElements());
            return ResponseEntity.ok(orders);
        } finally {
            MDC.remove("endpoint");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderRequest> updateOrder(
            @PathVariable String id,
            @Valid @RequestBody OrderRequest order) {

        MDC.put("endpoint", "PUT /api/orders/{id}");
        MDC.put("orderId", id);
        try {
            log.info("Request received");
            OrderRequest updated = orderService.updateOrderById(id, order);
            log.info("Request completed");
            return ResponseEntity.ok(updated);
        } finally {
            MDC.remove("orderId");
            MDC.remove("endpoint");
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderRequest> updatePartialOrderData(
            @PathVariable String id,
            @RequestBody OrderRequest order) {

        MDC.put("endpoint", "PATCH /api/orders/{id}");
        MDC.put("orderId", id);
        try {
            log.info("Request received");
            OrderRequest updated = orderService.updatePartialOrderData(id, order);
            log.info("Request completed");
            return ResponseEntity.ok(updated);
        } finally {
            MDC.remove("orderId");
            MDC.remove("endpoint");
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderRequest> updateStatus(
            @PathVariable String id,
            @RequestBody StatusRequest status) {

        MDC.put("endpoint", "PATCH /api/orders/{id}/status");
        MDC.put("orderId", id);
        try {
            log.info("Request received status={}", status.getStatus());
            OrderRequest updated = orderService.updateStatusById(id, status);
            log.info("Request completed");
            return ResponseEntity.ok(updated);
        } finally {
            MDC.remove("orderId");
            MDC.remove("endpoint");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        MDC.put("endpoint", "DELETE /api/orders/{id}");
        MDC.put("orderId", id);
        try {
            log.info("Request received");
            orderService.deleteOrderById(id);
            log.info("Request completed");
            return ResponseEntity.noContent().build();
        } finally {
            MDC.remove("orderId");
            MDC.remove("endpoint");
        }
    }
}