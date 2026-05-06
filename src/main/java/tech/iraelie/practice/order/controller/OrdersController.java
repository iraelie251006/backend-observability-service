package tech.iraelie.practice.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.iraelie.practice.order.dto.OrderCreateRequest;
import tech.iraelie.practice.order.dto.OrderRequest;
import tech.iraelie.practice.order.dto.StatusRequest;
import tech.iraelie.practice.order.service.OrderInterface;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrdersController {
    private final OrderInterface orderService;

    @PostMapping("/")
    public ResponseEntity<OrderRequest> createOrder(@Valid @RequestBody OrderCreateRequest order) {
        OrderRequest newOrder = orderService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED.value()).body(newOrder);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderRequest> getOrderById(@PathVariable String id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<List<OrderRequest>> getAllOrders() {
        List<OrderRequest> orders = orderService.getAllOrders();
        return ResponseEntity.ok().body(orders);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderRequest> updateOrder(@PathVariable String id, @Valid @RequestBody OrderRequest order) {
        return orderService.updateOrderById(id, order)
                .map(updatedOrder -> ResponseEntity.ok().body(updatedOrder))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteOrder(@PathVariable String id) {
        if (orderService.deleteOrderById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderRequest> updatePartialOrderData(@PathVariable String id, @RequestBody OrderRequest order) {
        return orderService.updatePartialOrderData(id, order)
                .map(updatedOrder -> ResponseEntity.ok().body(updatedOrder))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderRequest> updateStatus(@PathVariable String id, @RequestBody StatusRequest status) {
        return orderService.updateStatusById(id, status)
                .map(updatedOrder -> ResponseEntity.ok().body(updatedOrder))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
