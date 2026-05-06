package tech.iraelie.practice.user.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String id) {
        super(String.format("User \"%s\" not found", id));
    }
}
