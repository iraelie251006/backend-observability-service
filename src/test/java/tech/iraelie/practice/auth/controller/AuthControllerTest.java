package tech.iraelie.practice.auth.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
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
        @DisplayName("happy path — returns 201 and sets two cookies")
        void happyPath() throws Exception {
            when(authService.register(any())).thenReturn(FAKE_AUTH);

            RegisterRequest body = new RegisterRequest("Alice", "alice@gmail.com", "password123");

            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(body)))
                    // register now returns 201, not 200
                    .andExpect(status().isCreated())
                    .andExpect(result -> {
                        var cookies = result.getResponse().getHeaders("Set-Cookie");
                        assert cookies.stream().anyMatch(c -> c.startsWith("access_token="));
                        assert cookies.stream().anyMatch(c -> c.startsWith("refresh_token="));
                    });
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
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("blank username — returns 400")
        void blankUsername() throws Exception {
            RegisterRequest body = new RegisterRequest("", "alice@gmail.com", "password123");

            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("password too short — returns 400")
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
        @DisplayName("invalid email format — returns 400")
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

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        var cookies = result.getResponse().getHeaders("Set-Cookie");
                        assert cookies.stream().anyMatch(c -> c.startsWith("access_token="));
                        assert cookies.stream().anyMatch(c -> c.startsWith("refresh_token="));
                    });
        }

        @Test
        @DisplayName("wrong credentials — returns 401")
        void wrongCredentials() throws Exception {
            // BadCredentialsException is an AuthenticationException — Spring Security
            // intercepts it before GlobalExceptionHandler and returns 401, not 500
            when(authService.login(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            LoginRequest body = new LoginRequest("alice@example.com", "wrongpassword");

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("missing email field — returns 400")
        void missingEmail() throws Exception {
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

    // Rate limit testing belongs in an integration test where RateLimitFilter
    // actually runs in the servlet context — @WebMvcTest excludes it, so the
    // test was vacuous. Move this to a @SpringBootTest slice when you write
    // integration tests.
}