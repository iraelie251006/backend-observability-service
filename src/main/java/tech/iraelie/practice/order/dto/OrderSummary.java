package tech.iraelie.practice.order.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record OrderSummary(
        String id,
        Double totalAmount,
        OrderStatus orderStatus,
        LocalDateTime createdAt
) {}