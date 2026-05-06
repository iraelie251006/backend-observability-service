package tech.iraelie.practice.order.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.iraelie.practice.auth.service.JwtAuthFilter;
import tech.iraelie.practice.limit.RateLimitFilter;
import tech.iraelie.practice.order.dto.OrderCreateRequest;
import tech.iraelie.practice.order.dto.OrderRequest;
import tech.iraelie.practice.order.dto.OrderStatus;
import tech.iraelie.practice.order.service.OrderInterface;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OrdersController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthFilter.class, RateLimitFilter.class}
        )
)
class OrdersControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private OrderInterface orderService;

    private OrderRequest stubOrder() {
        return OrderRequest.builder()
                .id("order-1")
                .totalAmount(99.99)
                .orderStatus(OrderStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("Unauthenticated requests tests")
    class UnauthenticatedTest {
        @Test
        @DisplayName("GET /api/orders/all — 401 when not logged in")
        void getAllOrders_unauthorized() throws Exception {
            mockMvc.perform(get("/api/orders/all"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/orders/{id} — 401 when not logged in")
        void getById_unauthorized() throws Exception {
            mockMvc.perform(get("/api/orders/order-1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/orders/ — 401 when not logged in")
        void createOrder_unauthorized() throws Exception {
            mockMvc.perform(post("/api/orders/").with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DELETE /api/orders/{id} — 401 when not logged in")
        void deleteOrder_unauthorized() throws Exception {
            mockMvc.perform(delete("/api/orders/order-1").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Authenticated as USER role")
    class AuthenticatedUser {
        @Test
        @WithMockUser(username = "alice@gmail.com")
        @DisplayName("GET /api/orders/all — 200 with list of orders")
        void getAllOrders_returnsOk() throws Exception {
            Mockito.when(orderService.getAllOrders()).thenReturn(List.of(stubOrder()));

            mockMvc.perform(get("/api/orders/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("order-1"))
                    .andExpect(jsonPath("$[0].totalAmount").value(99.99));
        }

        @Test
        @WithMockUser(username = "alice@example.com")
        @DisplayName("GET /api/orders/{id} — 200 when order exists")
        void getById_returnsOk() throws Exception {
            when(orderService.getOrderById("order-1")).thenReturn(Optional.of(stubOrder()));

            mockMvc.perform(get("/api/orders/order-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("order-1"));
        }

        @Test
        @WithMockUser(username = "alice@example.com")
        @DisplayName("GET /api/orders/{id} — 404 when order not found")
        void getById_notFound() throws Exception {
            when(orderService.getOrderById("missing")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/orders/missing"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "alice@example.com")
        @DisplayName("POST /api/orders/ — 201 Created")
        void createOrder_returnsCreated() throws Exception {
            when(orderService.createOrder(any())).thenReturn(stubOrder());

            OrderCreateRequest req = OrderCreateRequest.builder()
                    .userId("user-1")
                    .totalAmount(99.99)
                    .orderStatus(OrderStatus.PENDING)
                    .build();

            mockMvc.perform(post("/api/orders/").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("order-1"));
        }

        @Test
        @WithMockUser(username = "alice@example.com")
        @DisplayName("DELETE /api/orders/{id} — 204 when deleted successfully")
        void deleteOrder_returnsNoContent() throws Exception {
            when(orderService.deleteOrderById("order-1")).thenReturn(true);

            mockMvc.perform(delete("/api/orders/order-1").with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "alice@example.com")
        @DisplayName("DELETE /api/orders/{id} — 404 when order doesn't exist")
        void deleteOrder_notFound() throws Exception {
            when(orderService.deleteOrderById("missing")).thenReturn(false);

            mockMvc.perform(delete("/api/orders/missing").with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Role-based access denial")
    class RoleBasedAccess {
        @Test
        @WithMockUser
        @DisplayName("GET /api/orders/all — 200 even with no explicit authorities (endpoint uses anyRequest().authenticated())")
        void noAuthoritiesStillPassesAuthenticatedCheck() throws Exception {
            when(orderService.getAllOrders()).thenReturn(List.of());

            mockMvc.perform(get("/api/orders/all"))
                    .andExpect(status().isOk());
        }
    }
}