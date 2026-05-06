package tech.iraelie.practice.auth.exception;

public class UserEmailAlreadyExistException extends RuntimeException {
    public UserEmailAlreadyExistException() {
        super("User email is already taken");
    }
}
