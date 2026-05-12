package tech.iraelie.practice.order.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.iraelie.practice.auth.service.JwtAuthFilter;
import tech.iraelie.practice.limit.RateLimitFilter;
import tech.iraelie.practice.order.dto.OrderCreateRequest;
import tech.iraelie.practice.order.dto.OrderRequest;
import tech.iraelie.practice.order.dto.OrderStatus;
import tech.iraelie.practice.order.exception.OrderNotFoundException;
import tech.iraelie.practice.order.service.OrderInterface;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
    @DisplayName("Unauthenticated requests")
    class UnauthenticatedTest {

        @Test
        @DisplayName("GET /api/orders/all — 401 when not authenticated")
        void getAllOrders_unauthorized() throws Exception {
            mockMvc.perform(get("/api/orders/all"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/orders/{id} — 401 when not authenticated")
        void getById_unauthorized() throws Exception {
            mockMvc.perform(get("/api/orders/order-1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/orders — 401 when not authenticated")
        void createOrder_unauthorized() throws Exception {
            // No trailing slash — fixed from original
            mockMvc.perform(post("/api/orders").with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DELETE /api/orders/{id} — 401 when not authenticated")
        void deleteOrder_unauthorized() throws Exception {
            mockMvc.perform(delete("/api/orders/order-1").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Authenticated requests")
    @WithMockUser(username = "alice@example.com")
    class AuthenticatedUser {

        @Test
        @DisplayName("GET /api/orders/all — 200 with paginated orders")
        void getAllOrders_returnsOk() throws Exception {
            // getAllOrders now takes Pageable — stub with any(Pageable)
            when(orderService.getAllOrders(any()))
                    .thenReturn(new PageImpl<>(List.of(stubOrder()), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/orders/all")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    // PageImpl serializes to { content: [...], totalElements: ..., ... }
                    .andExpect(jsonPath("$.content[0].id").value("order-1"))
                    .andExpect(jsonPath("$.content[0].totalAmount").value(99.99))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("GET /api/orders/{id} — 200 when order exists")
        void getById_returnsOk() throws Exception {
            // getOrderById now returns OrderRequest directly, not Optional
            when(orderService.getOrderById("order-1")).thenReturn(stubOrder());

            mockMvc.perform(get("/api/orders/order-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("order-1"));
        }

        @Test
        @DisplayName("GET /api/orders/{id} — 404 when order not found")
        void getById_notFound() throws Exception {
            // 404 is now driven by OrderNotFoundException, not Optional.empty()
            when(orderService.getOrderById("missing"))
                    .thenThrow(new OrderNotFoundException("missing"));

            mockMvc.perform(get("/api/orders/missing"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("POST /api/orders — 201 Created")
        void createOrder_returnsCreated() throws Exception {
            when(orderService.createOrder(any())).thenReturn(stubOrder());

            OrderCreateRequest req = OrderCreateRequest.builder()
                    .userId("user-1")
                    .totalAmount(99.99)
                    .orderStatus(OrderStatus.PENDING)
                    .build();

            // No trailing slash
            mockMvc.perform(post("/api/orders").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("order-1"));
        }

        @Test
        @DisplayName("DELETE /api/orders/{id} — 204 when deleted successfully")
        void deleteOrder_returnsNoContent() throws Exception {
            // deleteOrderById is now void — no stub needed for happy path
            mockMvc.perform(delete("/api/orders/order-1").with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("DELETE /api/orders/{id} — 404 when order not found")
        void deleteOrder_notFound() throws Exception {
            // 404 is now driven by OrderNotFoundException thrown from the service
            doThrow(new OrderNotFoundException("missing"))
                    .when(orderService).deleteOrderById("missing");

            mockMvc.perform(delete("/api/orders/missing").with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}