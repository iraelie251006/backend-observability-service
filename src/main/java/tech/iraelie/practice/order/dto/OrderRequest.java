package tech.iraelie.practice.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import tech.iraelie.practice.user.dto.UserDTO;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OrderRequest {
    private String id;
    private UserDTO user;
    private Double totalAmount;
    private OrderStatus orderStatus;
}
