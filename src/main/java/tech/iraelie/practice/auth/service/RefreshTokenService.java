package tech.iraelie.practice.auth.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.iraelie.practice.auth.dto.TokenPair;
import tech.iraelie.practice.auth.exception.TokenException;
import tech.iraelie.practice.auth.model.RefreshToken;
import tech.iraelie.practice.auth.repository.RefreshTokenRepository;
import tech.iraelie.practice.user.model.User;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${spring.security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public String createRefreshToken(User user) {
        return createRefreshToken(user, UUID.randomUUID().toString()); // new family
    }

    // ── Create within an existing family (called on rotation) ───────
    private String createRefreshToken(User user, String family) {
        String rawToken = UUID.randomUUID().toString();

        String hashedToken = DigestUtils.sha256Hex(rawToken);

        RefreshToken token = RefreshToken.builder()
                .tokenHash(hashedToken)
                .user(user)
                .family(family)
                .revoked(false)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        refreshTokenRepository.save(token);
        return rawToken;
    }

    public TokenPair rotateRefreshToken(String incomingToken) {

        String hash = DigestUtils.sha256Hex(incomingToken);

        // 1. Look up the token in the database
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new TokenException("Refresh token not found"));

        // 2. Check if it's already been revoked
        if (stored.isRevoked()) {
            // !! REUSE DETECTED !!
            // A revoked token was presented — someone is replaying an old token.
            // We don't know if it's the legitimate client or an attacker.
            // Safe assumption: theft occurred. Nuke the entire family.
            refreshTokenRepository.revokeAllByFamily(stored.getFamily());
            throw new TokenException(
                    "Refresh token reuse detected. All sessions invalidated."
            );
        }

        // 3. Check expiry
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new TokenException("Refresh token expired. Please log in again.");
        }

        // 4. Revoke the current token (rotate it out)
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        // 5. Issue a brand-new token in the same family
        String newRefreshToken =
                createRefreshToken(stored.getUser(), stored.getFamily());

        // 6. Generate a new access token
        String newAccessToken =
                jwtService.generateToken(stored.getUser());

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    // ── Logout — revoke all tokens for this user ─────────────────────
    public void revokeAllUserTokens(String userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
