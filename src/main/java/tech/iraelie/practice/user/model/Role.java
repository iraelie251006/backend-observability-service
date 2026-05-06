package tech.iraelie.practice.user.model;

import lombok.Getter;

import java.util.Set;

@Getter
public enum Role {
    USER(
            Set.of(
                    Permission.USER_READ,
                    Permission.ORDER_READ,
                    Permission.ORDER_UPDATE,
                    Permission.ORDER_DELETE,
                    Permission.ORDER_CREATE,
                    Permission.USER_UPDATE
            )
    ),
    ADMIN(
            Set.of(Permission.values())
    );

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
