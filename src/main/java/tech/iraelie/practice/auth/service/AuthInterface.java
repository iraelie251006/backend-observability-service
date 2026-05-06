package tech.iraelie.practice.auth.service;

import tech.iraelie.practice.auth.dto.AuthResponse;
import tech.iraelie.practice.auth.dto.LoginRequest;
import tech.iraelie.practice.auth.dto.RefreshRequest;
import tech.iraelie.practice.auth.dto.RegisterRequest;
import tech.iraelie.practice.user.model.User;

public interface AuthInterface {
    AuthResponse register(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);

    void logout(User userDetails);

    AuthResponse refresh(RefreshRequest request);
}
