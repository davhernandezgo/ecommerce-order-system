package com.b2chat.ecommerce.controller;

import com.b2chat.ecommerce.dto.request.CreateProductRequest;
import com.b2chat.ecommerce.dto.response.ProductResponse;
import com.b2chat.ecommerce.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ProductController Integration Tests")
class ProductControllerIntegrationTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  ProductService productService;

    @Test
    @DisplayName("GET /api/v1/products — public, returns product list")
    void getAllProducts_public_returns200() throws Exception {
        ProductResponse p = ProductResponse.builder()
                .id(1L).name("Widget").price(new BigDecimal("9.99")).stockQuantity(50).build();
        given(productService.getAllProducts()).willReturn(List.of(p));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Widget"));
    }

    @Test
    @DisplayName("POST /api/v1/products — 403 when not authenticated (JWT filter, no WWW-Authenticate)")
    void createProduct_unauthenticated_returns403() throws Exception {
        CreateProductRequest req = new CreateProductRequest();
        req.setName("X"); req.setPrice(BigDecimal.ONE); req.setStockQuantity(1);

        // With a stateless JWT-only security config (no HTTP Basic / form login),
        // Spring Security responds with 403 Forbidden rather than 401 Unauthorized
        // when no credentials are present at all.
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/products — 201 when authenticated with valid body")
    void createProduct_authenticated_returns201() throws Exception {
        CreateProductRequest req = new CreateProductRequest();
        req.setName("Widget"); req.setPrice(new BigDecimal("9.99")); req.setStockQuantity(10);

        ProductResponse saved = ProductResponse.builder()
                .id(1L).name("Widget").price(new BigDecimal("9.99")).stockQuantity(10).build();
        given(productService.createProduct(any())).willReturn(saved);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/v1/products/{id} — returns 200 for existing product")
    void deleteProduct_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
