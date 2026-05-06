package tech.iraelie.practice.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tech.iraelie.practice.auth.dto.AuthResponse;
import tech.iraelie.practice.auth.dto.LoginRequest;
import tech.iraelie.practice.auth.dto.RefreshRequest;
import tech.iraelie.practice.auth.dto.RegisterRequest;
import tech.iraelie.practice.auth.service.AuthInterface;
import tech.iraelie.practice.user.model.User;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthInterface authService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.register(registerRequest);
        setAuthCookies(response, authResponse.accessToken(), authResponse.refreshToken(), false);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> signInUser(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(loginRequest);
        setAuthCookies(response, authResponse.accessToken(), authResponse.refreshToken(), false);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthResponse authResponse = authService.refresh(new RefreshRequest(refreshToken));
        setAuthCookies(response, authResponse.accessToken(), authResponse.refreshToken(), false);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal User userDetails,
            HttpServletResponse response
    ) {
        authService.logout(userDetails);
        setAuthCookies(response, "", "", true);

        return ResponseEntity.noContent().build();
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken, boolean isLogout) {

        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true).secure(true).path("/")
                .maxAge(Duration.ofMinutes(isLogout ? 0 : 15)).sameSite("None").build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true).secure(true).path("/api/auth/refresh")
                .maxAge(Duration.ofDays(isLogout ? 0 : 7)).sameSite("None").build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}
