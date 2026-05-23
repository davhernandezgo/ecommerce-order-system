package com.b2chat.ecommerce.service;

import com.b2chat.ecommerce.dto.request.RegisterUserRequest;
import com.b2chat.ecommerce.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse registerUser(RegisterUserRequest request);
    UserResponse getUserById(Long id);
    Page<UserResponse> getOrdersByUser(Long userId, Pageable pageable);
}
