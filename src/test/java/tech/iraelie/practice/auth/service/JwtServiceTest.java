package tech.iraelie.practice.auth.service;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tech.iraelie.practice.user.model.Role;
import tech.iraelie.practice.user.model.User;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET =
            Base64.getEncoder().encodeToString("test-secret-key-must-be-32-bytes!!".getBytes());

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setup() {
        jwtService = new JwtService();
        // Field was renamed to secretKeyValue in the refactor
        ReflectionTestUtils.setField(jwtService, "secretKeyValue", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 900_000L);
        // @PostConstruct won't fire on a plain new — invoke it manually
        ReflectionTestUtils.invokeMethod(jwtService, "initSigningKey");

        user = User.builder()
                .id("user-id-1")
                .name("test-user")
                .email("hello@gmail.com")
                .password("encoded-password")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateTokenTest {

        @Test
        @DisplayName("returns a non-blank JWT string")
        void returnsNonBlankToken() {
            assertThat(jwtService.generateToken(user)).isNotBlank();
        }

        @Test
        @DisplayName("token subject equals the user email")
        void subjectIsEmail() {
            String token = jwtService.generateToken(user);
            assertThat(jwtService.extractUsername(token)).isEqualTo("hello@gmail.com");
        }

        @Test
        @DisplayName("token is valid immediately after generation")
        void freshTokenIsValid() {
            String token = jwtService.generateToken(user);
            // Replaces the old extractAllClaims test — assert observable behavior
            assertThat(jwtService.isTokenValid(token, user)).isTrue();
        }

        @Test
        @DisplayName("two tokens generated at different times are different strings")
        void tokensAreUnique() throws InterruptedException {
            String first = jwtService.generateToken(user);
            Thread.sleep(1_000);
            String second = jwtService.generateToken(user);
            assertThat(first).isNotEqualTo(second);
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValidTest {

        @Test
        @DisplayName("returns true for a fresh token belonging to the same user")
        void validTokenForSameUser() {
            String token = jwtService.generateToken(user);
            assertThat(jwtService.isTokenValid(token, user)).isTrue();
        }

        @Test
        @DisplayName("returns false when the token belongs to a different user")
        void invalidWhenUsernameMismatch() {
            User bob = User.builder()
                    .role(Role.USER)
                    .email("bob@gmail.com")
                    .name("Bob")
                    .password("encoded-password")
                    .build();

            String token = jwtService.generateToken(user);
            assertThat(jwtService.isTokenValid(token, bob)).isFalse();
        }

        @Test
        @DisplayName("returns false for an expired token")
        void returnsFalseWhenExpired() {
            ReflectionTestUtils.setField(jwtService, "expirationMs", -1L);
            String expired = jwtService.generateToken(user);
            assertThat(jwtService.isTokenValid(expired, user)).isFalse();
        }

        @Test
        @DisplayName("returns false for a token signed with a different key")
        void returnsFalseForWrongSignature() {
            JwtService otherService = new JwtService();
            String otherSecret = Base64.getEncoder()
                    .encodeToString("completely-different-secret-32b!".getBytes());
            ReflectionTestUtils.setField(otherService, "secretKeyValue", otherSecret);
            ReflectionTestUtils.setField(otherService, "expirationMs", 900_000L);
            ReflectionTestUtils.invokeMethod(otherService, "initSigningKey");

            String tokenFromOtherKey = otherService.generateToken(user);
            // isTokenValid catches JwtException internally — returns false, not throws
            assertThat(jwtService.isTokenValid(tokenFromOtherKey, user)).isFalse();
        }
    }

    @Nested
    @DisplayName("extractUsername — invalid token rejection")
    class InvalidTokenTest {

        @Test
        @DisplayName("throws JwtException for a token signed with a different key")
        void rejectsWrongSignature() {
            JwtService otherService = new JwtService();
            String otherSecret = Base64.getEncoder()
                    .encodeToString("completely-different-secret-32b!".getBytes());
            ReflectionTestUtils.setField(otherService, "secretKeyValue", otherSecret);
            ReflectionTestUtils.setField(otherService, "expirationMs", 900_000L);
            ReflectionTestUtils.invokeMethod(otherService, "initSigningKey");

            String foreignToken = otherService.generateToken(user);
            assertThatThrownBy(() -> jwtService.extractUsername(foreignToken))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("throws JwtException for a tampered token")
        void rejectsTamperedToken() {
            String token = jwtService.generateToken(user);
            String tampered = token.substring(0, token.length() - 1) + "X";
            assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("throws JwtException for a completely random string")
        void rejectsGarbage() {
            assertThatThrownBy(() -> jwtService.extractUsername("not.a.jwt"))
                    .isInstanceOf(JwtException.class);
        }
    }
}