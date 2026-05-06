package tech.iraelie.practice.order.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELED;

    @JsonCreator
    public static OrderStatus from(String value) {
            return OrderStatus.valueOf(value.toUpperCase());
    }
}
