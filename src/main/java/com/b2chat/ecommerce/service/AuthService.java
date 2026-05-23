package com.b2chat.ecommerce.service;

import com.b2chat.ecommerce.dto.request.LoginRequest;
import com.b2chat.ecommerce.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}
