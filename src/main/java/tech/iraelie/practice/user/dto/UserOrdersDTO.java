package tech.iraelie.practice.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import tech.iraelie.practice.order.dto.OrderSummary;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class UserOrdersDTO {
    private List<OrderSummary> orders;
}
