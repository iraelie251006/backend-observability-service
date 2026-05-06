package tech.iraelie.practice.user.services;


import tech.iraelie.practice.user.dto.UserDTO;
import tech.iraelie.practice.user.dto.UserOrdersDTO;

import java.util.List;

public interface UserInterface {
    List<UserDTO> getAllUsers();

    UserDTO getUserById(String id);

    UserOrdersDTO getOrdersByUser(String id);
}
