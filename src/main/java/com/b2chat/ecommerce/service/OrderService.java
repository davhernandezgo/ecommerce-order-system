package com.b2chat.ecommerce.service;

import com.b2chat.ecommerce.dto.request.CreateOrderRequest;
import com.b2chat.ecommerce.dto.request.UpdateOrderStatusRequest;
import com.b2chat.ecommerce.dto.response.OrderResponse;
import com.b2chat.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse placeOrder(CreateOrderRequest request);
    OrderResponse getOrderById(Long id);
    OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request);
    Page<OrderResponse> getOrdersByUser(Long userId, Pageable pageable);
}
