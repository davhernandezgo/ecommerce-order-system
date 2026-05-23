package com.b2chat.ecommerce.controller;

import com.b2chat.ecommerce.dto.request.RegisterUserRequest;
import com.b2chat.ecommerce.dto.response.UserResponse;
import com.b2chat.ecommerce.service.UserService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  UserService userService;

    @Test
    @DisplayName("POST /api/v1/users — registers user and returns 201")
    void registerUser_returns201() throws Exception {
        RegisterUserRequest req = new RegisterUserRequest();
        req.setName("Alice"); req.setEmail("alice@x.com");
        req.setAddress("NY"); req.setPassword("pass123");

        UserResponse resp = UserResponse.builder().id(1L).name("Alice").email("alice@x.com").build();
        given(userService.registerUser(any())).willReturn(resp);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alice@x.com"));
    }

    @Test
    @DisplayName("POST /api/v1/users — returns 400 for missing fields")
    void registerUser_invalidInput_returns400() throws Exception {
        RegisterUserRequest req = new RegisterUserRequest(); // empty

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/users/{id} — returns user when authenticated")
    void getUserById_authenticated_returns200() throws Exception {
        UserResponse resp = UserResponse.builder().id(5L).name("Bob").email("bob@x.com").build();
        given(userService.getUserById(5L)).willReturn(resp);

        mockMvc.perform(get("/api/v1/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5));
    }
}
