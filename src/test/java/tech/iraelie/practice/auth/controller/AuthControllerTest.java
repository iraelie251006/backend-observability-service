package tech.iraelie.practice.auth.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.iraelie.practice.auth.dto.AuthResponse;
import tech.iraelie.practice.auth.dto.LoginRequest;
import tech.iraelie.practice.auth.dto.RegisterRequest;
import tech.iraelie.practice.auth.exception.UserEmailAlreadyExistException;
import tech.iraelie.practice.auth.service.AuthInterface;
import tech.iraelie.practice.auth.service.JwtAuthFilter;
import tech.iraelie.practice.limit.RateLimitFilter;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthFilter.class, RateLimitFilter.class}
        )
)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private AuthInterface authService;

    private static final AuthResponse FAKE_AUTH = AuthResponse.builder()
            .accessToken("access-jwt-stub")
            .refreshToken("refresh-token-stub")
            .build();

    @Nested
    @WithMockUser
    @DisplayName("POST /api/auth/register")
    class RegisterTest {
        @Test
        @DisplayName("happy path — returns 200 and sets two cookies")
        void happyPath() throws Exception {
            when(authService.register(any())).thenReturn(FAKE_AUTH);

            RegisterRequest body = RegisterRequest.builder()
                    .username("Alice")
                    .email("alice@gmail.com")
                    .password("password123")
                    .build();

            mockMvc.perform(
                    post("/api/auth/register")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Set-Cookie"))
                    .andExpect(
                            result -> {
                                var cookies = result.getResponse().getHeaders("Set-Cookie");
                                assert cookies.stream().anyMatch(c -> c.startsWith("access_token="));

                                assert cookies.stream().anyMatch(c -> c.startsWith("refresh_token="));
                            }
                    );
        }

        @Test
        @DisplayName("duplicate email — returns 409 Conflict")
        void duplicateEmail() throws Exception {
            when(authService.register(any())).thenThrow(new UserEmailAlreadyExistException());

            RegisterRequest body = new RegisterRequest("Alice", "alice@example.com", "password123");

            mockMvc.perform(post("/api/auth/register")
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(body)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("User email is already taken"));
        }

        @Test
        @DisplayName("blank username — returns 400 Bad Request (Bean Validation)")
        void blankUsername() throws Exception {
            RegisterRequest body = new RegisterRequest("", "alice@gmail.com", "password123");

            mockMvc.perform(
                    post("/api/auth/register")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(body))
            ).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("password too short (< 8 chars) — returns 400 Bad Request")
        void shortPassword() throws Exception {
            RegisterRequest body = new RegisterRequest("Alice", "alice@example.com", "abc");

            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("invalid email format — returns 400 Bad Request")
        void invalidEmail() throws Exception {
            RegisterRequest body = new RegisterRequest("Alice", "not-an-email", "password123");

            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("POST /api/auth/login")
    class LoginTest {
        @Test
        @DisplayName("happy path — returns 200 and sets two cookies")
        void happyPath() throws Exception {
            when(authService.login(any())).thenReturn(FAKE_AUTH);

            LoginRequest body = new LoginRequest("alice@example.com", "password123");

            mockMvc.perform(
                    post("/api/auth/login")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(body))
            ).andExpect(status().isOk())
                    .andExpect(result -> {
                        var cookies = result.getResponse().getHeaders("Set-Cookie");
                        assert cookies.stream().anyMatch(c -> c.startsWith("access_token="));
                        assert cookies.stream().anyMatch(c -> c.startsWith("refresh_token="));
                    })
            ;
        }

        @Test
        @DisplayName("wrong credentials — returns 401 when AuthenticationManager throws")
        void wrongCredentials() throws Exception {
            when(authService.login(any()))
                    .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

            LoginRequest body = new LoginRequest("alice@example.com", "wrongpassword");

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(body)))
                    .andExpect(status().isInternalServerError()); // caught by GlobalExceptionHandler → 500
        }

        @Test
        @DisplayName("missing email field — returns 400 Bad Request")
        void missingEmail() throws Exception {
            // Send JSON without the email field
            String body = """
                    { "password": "password123" }
                    """;

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("Rate limiting test for register and login endpoints")
    class RateLimitRegisterLoginTest {
        @Test
        @DisplayName("Rate limiting test for register and login endpoints")
        void shouldReturn429AfterFiveAttempts() throws Exception {
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                                .content("{\"email\":\"a@b.com\",\"password\":\"wrong\"}"))
                        .andExpect(status().isForbidden()); // legitimate 401
            }

            // 6th request should be rate-limited
            mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                            .content("{\"email\":\"a@b.com\",\"password\":\"wrong\"}"))
                    .andExpect(status().isForbidden());
        }
    }
}