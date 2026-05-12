package tech.iraelie.practice.user.services;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tech.iraelie.practice.user.dto.UserDTO;
import tech.iraelie.practice.user.dto.UserOrdersDTO;

import java.util.List;

public interface UserInterface {
    Page<UserDTO> getAllUsers(Pageable pageable);

    UserDTO getUserById(String id);

    UserOrdersDTO getOrdersByUser(String id);
}
