package tech.iraelie.practice.user.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tech.iraelie.practice.order.dto.OrderSummary;
import tech.iraelie.practice.user.model.User;
import tech.iraelie.practice.user.repository.UserRepository;
import tech.iraelie.practice.user.dto.UserDTO;
import tech.iraelie.practice.user.dto.UserOrdersDTO;
import tech.iraelie.practice.user.exception.UserNotFoundException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserInterface, UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "usersPage", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        log.info("Fetching users page={} size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<UserDTO> page = userRepository.findAll(pageable)
                .map(user -> UserDTO.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .createdAt(user.getCreatedAt())
                        .build());

        log.info("Fetched users page={} totalElements={}", pageable.getPageNumber(), page.getTotalElements());
        return page;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @Cacheable(value = "users", key = "#id")
    public UserDTO getUserById(String id) {
        log.info("Fetching user userId={}", id);

        UserDTO user = userRepository.findById(id)
                .map(u -> UserDTO.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .name(u.getName())
                        .createdAt(u.getCreatedAt())
                        .build())
                .orElseThrow(() -> {
                    log.warn("User not found userId={}", id);
                    return new UserNotFoundException(id);
                });

        log.debug("Resolved user userId={} email={}", user.getId(), user.getEmail());
        return user;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @Cacheable(value = "userOrders", key = "#id")
    public UserOrdersDTO getOrdersByUser(String id) {
        log.info("Fetching orders for userId={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found during order lookup userId={}", id);
                    return new UserNotFoundException(id);
                });

        List<OrderSummary> orders = user.getOrders()
                .stream()
                .map(order -> OrderSummary.builder()
                        .id(order.getId())
                        .totalAmount(order.getTotalAmount())
                        .orderStatus(order.getOrderStatus())
                        .createdAt(order.getCreatedAt())
                        .build())
                .toList();

        log.info("Resolved orders for userId={} orderCount={}", id, orders.size());
        return UserOrdersDTO.builder().orders(orders).build();
    }

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        log.debug("Loading user by email email=***");

        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Authentication failed: no user found for supplied email");
                    return new UsernameNotFoundException("No user found with email: " + email);
                });
    }
}