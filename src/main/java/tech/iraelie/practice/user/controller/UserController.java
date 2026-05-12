package tech.iraelie.practice.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.iraelie.practice.user.dto.UserDTO;
import tech.iraelie.practice.user.dto.UserOrdersDTO;
import tech.iraelie.practice.user.services.UserInterface;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserInterface userService;

    @GetMapping("/all")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        MDC.put("endpoint", "GET /api/users/all");
        try {
            log.info("Request received page={} size={} sortBy={} direction={}", page, size, sortBy, direction);
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<UserDTO> users = userService.getAllUsers(pageable);
            log.info("Request completed totalElements={} totalPages={}", users.getTotalElements(), users.getTotalPages());
            return ResponseEntity.ok(users);
        } finally {
            MDC.remove("endpoint");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable String id) {
        MDC.put("endpoint", "GET /api/users/{id}");
        MDC.put("userId", id);
        try {
            log.info("Request received");
            UserDTO user = userService.getUserById(id);
            log.info("Request completed");
            return ResponseEntity.ok(user);
        } finally {
            MDC.remove("userId");
            MDC.remove("endpoint");
        }
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<UserOrdersDTO> getUserOrders(@PathVariable String id) {
        MDC.put("endpoint", "GET /api/users/{id}/orders");
        MDC.put("userId", id);
        try {
            log.info("Request received");
            UserOrdersDTO orders = userService.getOrdersByUser(id);
            log.info("Request completed orderCount={}", orders.getOrders().size());
            return ResponseEntity.ok(orders);
        } finally {
            MDC.remove("userId");
            MDC.remove("endpoint");
        }
    }
}