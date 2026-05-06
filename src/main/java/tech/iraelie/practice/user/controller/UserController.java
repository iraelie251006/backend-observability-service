package tech.iraelie.practice.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.iraelie.practice.user.services.UserInterface;
import tech.iraelie.practice.user.dto.UserDTO;
import tech.iraelie.practice.user.dto.UserOrdersDTO;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserInterface userService;

    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.status(HttpStatus.OK.value()).body(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable String id) {
            return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<UserOrdersDTO> getUserOrders(@PathVariable String id) {
        UserOrdersDTO orders = userService.getOrdersByUser(id);
        return ResponseEntity.status(HttpStatus.OK.value()).body(orders);
    }

}
