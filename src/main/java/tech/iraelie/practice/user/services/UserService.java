package tech.iraelie.practice.user.services;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.NonNull;
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

@Service
@RequiredArgsConstructor
public class UserService implements UserInterface, UserDetailsService {
    private final UserRepository userRepository;
    
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream().map(
                        user -> UserDTO.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .name(user.getName())
                                .createdAt(user.getCreatedAt())
                                .build()
                ).toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public UserDTO getUserById(String id) {
        return userRepository.findById(id)
                .map(user -> UserDTO.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .createdAt(user.getCreatedAt())
                        .build()
                ).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public UserOrdersDTO getOrdersByUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        List<OrderSummary> orders = user.getOrders()
                .stream()
                .map(
                        order -> OrderSummary.builder()
                                .id(order.getId())
                                .totalAmount(order.getTotalAmount())
                                .orderStatus(order.getOrderStatus())
                                .createdAt(order.getCreatedAt())
                                .build()
                )
                .toList();

        return UserOrdersDTO.builder().orders(orders).build();
    }

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new UsernameNotFoundException("No user found with email: " + email)
                );
    }
}
