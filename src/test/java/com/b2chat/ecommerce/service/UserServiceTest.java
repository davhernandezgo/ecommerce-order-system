package com.b2chat.ecommerce.service;

import com.b2chat.ecommerce.dto.request.RegisterUserRequest;
import com.b2chat.ecommerce.dto.response.UserResponse;
import com.b2chat.ecommerce.entity.User;
import com.b2chat.ecommerce.exception.DuplicateResourceException;
import com.b2chat.ecommerce.exception.ResourceNotFoundException;
import com.b2chat.ecommerce.repository.OrderRepository;
import com.b2chat.ecommerce.repository.UserRepository;
import com.b2chat.ecommerce.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock UserRepository  userRepository;
    @Mock OrderRepository orderRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserServiceImpl userService;

    private RegisterUserRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterUserRequest();
        validRequest.setName("Alice Doe");
        validRequest.setEmail("alice@example.com");
        validRequest.setAddress("123 Main St");
        validRequest.setPassword("secret123");
    }

    @Test
    @DisplayName("registerUser — happy path")
    void registerUser_success() {
        given(userRepository.existsByEmail(validRequest.getEmail())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("hashed");

        User savedUser = User.builder()
                .id(1L).name(validRequest.getName())
                .email(validRequest.getEmail())
                .address(validRequest.getAddress())
                .password("hashed").role(User.Role.USER)
                .build();
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        UserResponse response = userService.registerUser(validRequest);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("registerUser — throws DuplicateResourceException for duplicate email")
    void registerUser_duplicateEmail_throwsException() {
        given(userRepository.existsByEmail(validRequest.getEmail())).willReturn(true);

        assertThatThrownBy(() -> userService.registerUser(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("alice@example.com");

        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("getUserById — returns user when found")
    void getUserById_found() {
        User user = User.builder().id(42L).name("Bob").email("bob@x.com")
                .address("NY").password("p").role(User.Role.USER).build();
        given(userRepository.findById(42L)).willReturn(Optional.of(user));

        UserResponse response = userService.getUserById(42L);

        assertThat(response.getId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("getUserById — throws ResourceNotFoundException when not found")
    void getUserById_notFound_throwsException() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
