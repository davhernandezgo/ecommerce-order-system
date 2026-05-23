package com.b2chat.ecommerce.service;

import com.b2chat.ecommerce.dto.request.CreateOrderRequest;
import com.b2chat.ecommerce.dto.request.UpdateOrderStatusRequest;
import com.b2chat.ecommerce.dto.response.OrderResponse;
import com.b2chat.ecommerce.entity.Order;
import com.b2chat.ecommerce.entity.Product;
import com.b2chat.ecommerce.entity.User;
import com.b2chat.ecommerce.exception.InsufficientStockException;
import com.b2chat.ecommerce.exception.InvalidOrderStatusTransitionException;
import com.b2chat.ecommerce.exception.ResourceNotFoundException;
import com.b2chat.ecommerce.repository.OrderRepository;
import com.b2chat.ecommerce.repository.ProductRepository;
import com.b2chat.ecommerce.repository.UserRepository;
import com.b2chat.ecommerce.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock OrderRepository     orderRepository;
    @Mock UserRepository      userRepository;
    @Mock ProductRepository   productRepository;
    @Mock NotificationService notificationService;

    @InjectMocks OrderServiceImpl orderService;

    private User        user;
    private Product     product;
    private CreateOrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Alice").email("alice@x.com")
                .address("NY").password("p").role(User.Role.USER).build();

        product = Product.builder().id(10L).name("Widget")
                .price(new BigDecimal("29.99")).stockQuantity(50).build();

        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest();
        itemReq.setProductId(10L);
        itemReq.setQuantity(2);

        orderRequest = new CreateOrderRequest();
        orderRequest.setUserId(1L);
        orderRequest.setItems(List.of(itemReq));
    }

    @Test
    @DisplayName("placeOrder — happy path deducts stock and saves order")
    void placeOrder_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(productRepository.findAllByIdWithLock(List.of(10L))).willReturn(List.of(product));

        Order savedOrder = Order.builder().id(100L).user(user)
                .status(Order.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("59.98"))
                .items(new ArrayList<>())
                .build();
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        OrderResponse response = orderService.placeOrder(orderRequest);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(product.getStockQuantity()).isEqualTo(48); // deducted 2
        then(notificationService).should().sendOrderReceivedNotification(any(Order.class));
    }

    @Test
    @DisplayName("placeOrder — throws InsufficientStockException when stock is low")
    void placeOrder_insufficientStock() {
        product.setStockQuantity(1); // only 1, but we request 2
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(productRepository.findAllByIdWithLock(List.of(10L))).willReturn(List.of(product));

        assertThatThrownBy(() -> orderService.placeOrder(orderRequest))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Widget");

        then(orderRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("placeOrder — throws ResourceNotFoundException for unknown user")
    void placeOrder_unknownUser() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(orderRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateOrderStatus — valid transition PENDING -> PROCESSING")
    void updateOrderStatus_validTransition() {
        Order order = Order.builder().id(1L).user(user)
                .status(Order.OrderStatus.PENDING).items(new ArrayList<>())
                .totalAmount(BigDecimal.TEN).build();

        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(order));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest();
        req.setStatus(Order.OrderStatus.PROCESSING);

        OrderResponse response = orderService.updateOrderStatus(1L, req);

        assertThat(response.getStatus()).isEqualTo(Order.OrderStatus.PROCESSING);
    }

    @Test
    @DisplayName("updateOrderStatus — throws on invalid transition COMPLETED -> PENDING")
    void updateOrderStatus_invalidTransition() {
        Order order = Order.builder().id(1L).user(user)
                .status(Order.OrderStatus.COMPLETED).items(new ArrayList<>())
                .totalAmount(BigDecimal.TEN).build();

        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(order));

        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest();
        req.setStatus(Order.OrderStatus.PENDING);

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, req))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }
}
