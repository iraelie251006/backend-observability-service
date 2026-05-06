package tech.iraelie.practice.user.model;

import lombok.Getter;

@Getter
public enum Permission {
    USER_READ("user:read"),
    USER_CREATE("user:create"),
    USER_UPDATE("user:update"),
    USER_DELETE("user:delete"),
    ORDER_READ("order:read"),
    ORDER_CREATE("order:create"),
    ORDER_UPDATE("order:update"),
    ORDER_DELETE("order:delete");


    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

}
