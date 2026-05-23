package com.b2chat.ecommerce.service.impl;

import com.b2chat.ecommerce.dto.request.RegisterUserRequest;
import com.b2chat.ecommerce.dto.response.OrderResponse;
import com.b2chat.ecommerce.dto.response.UserResponse;
import com.b2chat.ecommerce.entity.User;
import com.b2chat.ecommerce.exception.DuplicateResourceException;
import com.b2chat.ecommerce.exception.ResourceNotFoundException;
import com.b2chat.ecommerce.repository.OrderRepository;
import com.b2chat.ecommerce.repository.UserRepository;
import com.b2chat.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for user management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse registerUser(RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "A user with email '" + request.getEmail() + "' already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .address(request.getAddress())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {} (id={})", saved.getEmail(), saved.getId());
        return UserResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getOrdersByUser(Long userId, Pageable pageable) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        // Delegated — return users for now (orders returned via OrderService)
        return Page.empty(pageable);
    }
}
