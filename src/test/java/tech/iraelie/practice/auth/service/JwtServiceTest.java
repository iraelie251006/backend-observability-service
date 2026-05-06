package tech.iraelie.practice.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tech.iraelie.practice.user.model.Role;
import tech.iraelie.practice.user.model.User;

import static org.assertj.core.api.Assertions.*;

import java.util.Base64;

@DisplayName("JWT service test")
class JwtServiceTest {
    private static final String SECRET =
            Base64.getEncoder().encodeToString("test-secret-key-must-be-32-bytes!!".getBytes());

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setup() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 900_000);

        this.user = User.builder()
                .id("user-id-1")
                .name("test-user")
                .email("hello@gmail.com")
                .password("encoded-password")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("generate token tests class")
    class GenerateTokenTest {
        @Test
        @DisplayName("Returns a non-blank JWT string")
        void returnsNonBlankToken() {
            User user = JwtServiceTest.this.user;
            String token = jwtService.generateToken(user);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("token subject equals the user email (username)")
        void subjectIsEmail() {
            User user = JwtServiceTest.this.user;
            String token = jwtService.generateToken(user);
            String subject = jwtService.extractUsername(token);
            assertThat("hello@gmail.com").isEqualTo(subject);
        }

        @Test
        @DisplayName("token embeds the user's authorities")
        void containsAuthorities() {
            User user = JwtServiceTest.this.user;
            String token = jwtService.generateToken(user);
            Claims claims = jwtService.extractAllClaims(token);

            assertThat(claims.get("authorities")).isNotNull();
        }

        @Test
        @DisplayName("two tokens generated at different times are different strings")
        void tokensAreUnique() throws InterruptedException {
            User user = JwtServiceTest.this.user;
            String firstToken = jwtService.generateToken(user);
            Thread.sleep(1000);
            String secondToken = jwtService.generateToken(user);

            assertThat(firstToken).isNotEqualTo(secondToken);
        }
    }

    @Nested
    @DisplayName("is Token Valid tests class")
    class isTokenValidTest {
        @Test
        @DisplayName("returns true for a fresh token belonging to the same user")
        void validTokenForSameUser() {
            User user = JwtServiceTest.this.user;
            String token = jwtService.generateToken(user);
            boolean isTokenValid = jwtService.isTokenValid(token, user);

            assertThat(isTokenValid).isTrue();
        }

        @Test
        @DisplayName("returns false when the username in the token doesn't match the supplied user")
        void invalidWhenUsernameMismatch() {
            User hello = JwtServiceTest.this.user;
            User bob = User.builder()
                    .role(Role.USER)
                    .email("bob@gmail.com")
                    .name("Bob")
                    .password("encoded-password")
                    .build();

            String token = jwtService.generateToken(hello);
            assertThat(jwtService.isTokenValid(token, bob)).isFalse();
        }
    }

    @Nested
    @DisplayName("is Token Expired tests")
    class IsTokenExpired {
        @Test
        @DisplayName("fresh token is NOT expired")
        void freshTokenNotExpired() {
            User user = JwtServiceTest.this.user;
            String token = jwtService.generateToken(user);
            assertThat(jwtService.isTokenExpired(token)).isFalse();
        }

        @Test
        @DisplayName("token with expiration in the past IS expired")
        void expiredTokenIsDetected() {
            // Set expiration to -1 ms so the token is already expired when created
            ReflectionTestUtils.setField(jwtService, "expirationMs", -1L);

            User user = JwtServiceTest.this.user;
            String expiredToken = jwtService.generateToken(user);

            assertThat(jwtService.isTokenExpired(expiredToken)).isTrue();
        }

        @Test
        @DisplayName("isTokenValid returns false for an expired token")
        void isTokenValidReturnsFalseWhenExpired() {
            ReflectionTestUtils.setField(jwtService, "expirationMs", -1L);

            User user = JwtServiceTest.this.user;
            String expiredToken = jwtService.generateToken(user);

            assertThat(jwtService.isTokenValid(expiredToken, user)).isFalse();
        }
    }

    @Nested
    @DisplayName("invalid signature tests")
    class InvalidSignature {

        @Test
        @DisplayName("extractUsername throws JwtException for a token signed with a different key")
        void rejectsTokenSignedWithWrongKey() {
            // Build a second JwtService with a DIFFERENT secret
            JwtService otherService = new JwtService();
            String otherSecret = Base64.getEncoder()
                    .encodeToString("completely-different-secret-32b!".getBytes());
            ReflectionTestUtils.setField(otherService, "secretKey",     otherSecret);
            ReflectionTestUtils.setField(otherService, "expirationMs",  900_000L);

            User user  = JwtServiceTest.this.user;
            String tokenFromOtherKey = otherService.generateToken(user);

            // Our jwtService should reject it
            assertThatThrownBy(() -> jwtService.extractUsername(tokenFromOtherKey))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("extractUsername throws JwtException for a manually tampered token")
        void rejectsTamperedToken() {
            User user  = JwtServiceTest.this.user;
            String token = jwtService.generateToken(user);

            // Flip the last character to corrupt the signature
            String tampered = token.substring(0, token.length() - 1) + "X";

            assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("extractUsername throws JwtException for a completely random string")
        void rejectsGarbage() {
            assertThatThrownBy(() -> jwtService.extractUsername("not.a.jwt"))
                    .isInstanceOf(JwtException.class);
        }
    }
}