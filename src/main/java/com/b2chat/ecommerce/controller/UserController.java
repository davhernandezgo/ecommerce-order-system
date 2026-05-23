package com.b2chat.ecommerce.controller;

import com.b2chat.ecommerce.dto.request.LoginRequest;
import com.b2chat.ecommerce.dto.request.RegisterUserRequest;
import com.b2chat.ecommerce.dto.response.ApiResponse;
import com.b2chat.ecommerce.dto.response.AuthResponse;
import com.b2chat.ecommerce.dto.response.OrderResponse;
import com.b2chat.ecommerce.dto.response.UserResponse;
import com.b2chat.ecommerce.service.AuthService;
import com.b2chat.ecommerce.service.OrderService;
import com.b2chat.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user registration and retrieval.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService  userService;
    private final AuthService  authService;
    private final OrderService orderService;

    // ── Auth ────────────────────────────────────────────────────────────────────

    @PostMapping("/auth/login")
    @Operation(summary = "Authenticate and receive a JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse auth = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", auth));
    }

    // ── Users ───────────────────────────────────────────────────────────────────

    @PostMapping("/users")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @Valid @RequestBody RegisterUserRequest request) {
        UserResponse user = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", user));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Fetch user details by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    // ── Bonus: user orders ───────────────────────────────────────────────────────

    @GetMapping("/users/{id}/orders")
    @Operation(summary = "Fetch all orders for a user (paginated)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrders(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<OrderResponse> orders = orderService.getOrdersByUser(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
