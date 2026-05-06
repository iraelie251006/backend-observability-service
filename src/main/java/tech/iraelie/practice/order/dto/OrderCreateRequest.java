package tech.iraelie.practice.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OrderCreateRequest {
    private String userId;
    private Double totalAmount;
    private OrderStatus orderStatus;
}
