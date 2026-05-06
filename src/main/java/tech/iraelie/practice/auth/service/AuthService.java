package tech.iraelie.practice.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.iraelie.practice.auth.dto.*;
import tech.iraelie.practice.auth.exception.UserEmailAlreadyExistException;
import tech.iraelie.practice.user.exception.UserNotFoundException;
import tech.iraelie.practice.user.model.User;
import tech.iraelie.practice.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthInterface{
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.email().trim().toLowerCase())) {
            throw new UserEmailAlreadyExistException();
        }

        User userDetails = User.builder()
                .name(registerRequest.username())
                .email(registerRequest.email().trim().toLowerCase())
                .password(passwordEncoder.encode(registerRequest.password()))
                .build();

        userRepository.save(userDetails);

        return AuthResponse.builder()
                .accessToken(generateJwtToken(userDetails))
                .refreshToken(generateRefreshToken(userDetails))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email().trim().toLowerCase(),
                        loginRequest.password()
                )
        );

        User userDetails = (User) auth.getPrincipal();

        return AuthResponse.builder()
                .accessToken(generateJwtToken(userDetails))
                .refreshToken(generateRefreshToken(userDetails))
                .build();
    }

    @Override
    public void logout(User userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException(userDetails.getId()));

        refreshTokenService.revokeAllUserTokens(user.getId());
    }

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        TokenPair pair = refreshTokenService.rotateRefreshToken(request.refreshToken());

        return AuthResponse.builder()
                .accessToken(pair.accessToken())
                .refreshToken(pair.refreshToken())
                .build();
    }

    private String generateJwtToken(User userDetails) {
        return jwtService.generateToken(userDetails);
    }

    private String generateRefreshToken(User userDetails) {
        return refreshTokenService.createRefreshToken(userDetails);
    }
}
