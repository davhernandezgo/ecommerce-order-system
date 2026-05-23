package com.b2chat.ecommerce.service.impl;

import com.b2chat.ecommerce.dto.request.CreateOrderRequest;
import com.b2chat.ecommerce.dto.request.UpdateOrderStatusRequest;
import com.b2chat.ecommerce.dto.response.OrderResponse;
import com.b2chat.ecommerce.entity.Order;
import com.b2chat.ecommerce.entity.OrderItem;
import com.b2chat.ecommerce.entity.Product;
import com.b2chat.ecommerce.entity.User;
import com.b2chat.ecommerce.exception.InsufficientStockException;
import com.b2chat.ecommerce.exception.InvalidOrderStatusTransitionException;
import com.b2chat.ecommerce.exception.ResourceNotFoundException;
import com.b2chat.ecommerce.repository.OrderRepository;
import com.b2chat.ecommerce.repository.ProductRepository;
import com.b2chat.ecommerce.repository.UserRepository;
import com.b2chat.ecommerce.service.NotificationService;
import com.b2chat.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for order placement and lifecycle management.
 *
 * Stock deduction is protected with pessimistic write locks to prevent
 * over-selling under concurrent load.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository     orderRepository;
    private final UserRepository      userRepository;
    private final ProductRepository   productRepository;
    private final NotificationService notificationService;

    // ---------------------------------------------------------------------------
    // Allowed status transitions
    // ---------------------------------------------------------------------------
    private static final Map<Order.OrderStatus, Set<Order.OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            Order.OrderStatus.PENDING,    Set.of(Order.OrderStatus.PROCESSING, Order.OrderStatus.CANCELLED),
            Order.OrderStatus.PROCESSING, Set.of(Order.OrderStatus.COMPLETED,  Order.OrderStatus.CANCELLED),
            Order.OrderStatus.COMPLETED,  Set.of(),
            Order.OrderStatus.CANCELLED,  Set.of()
    );

    @Override
    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest request) {
        // 1. Validate user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        // 2. Collect product IDs and lock rows
        List<Long> productIds = request.getItems().stream()
                .map(CreateOrderRequest.OrderItemRequest::getProductId)
                .distinct()
                .collect(Collectors.toList());

        List<Product> lockedProducts = productRepository.findAllByIdWithLock(productIds);

        // Ensure all requested products were found
        if (lockedProducts.size() != productIds.size()) {
            Set<Long> foundIds = lockedProducts.stream().map(Product::getId).collect(Collectors.toSet());
            productIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .ifPresent(id -> { throw new ResourceNotFoundException("Product", id); });
        }

        Map<Long, Product> productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 3. Build order
        Order order = Order.builder()
                .user(user)
                .status(Order.OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productMap.get(itemReq.getProductId());

            // 4. Validate stock
            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new InsufficientStockException(
                        product.getName(), itemReq.getQuantity(), product.getStockQuantity());
            }

            // 5. Deduct stock
            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());

            // 6. Build order item (price snapshot at order time)
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();

            order.getItems().add(item);
            total = total.add(item.getSubtotal());
        }

        order.setTotalAmount(total);

        // 7. Persist (cascade saves OrderItems)
        Order saved = orderRepository.save(order);
        log.info("Order placed: id={}, userId={}, total={}", saved.getId(), user.getId(), total);

        // 8. Async notification on reception
        notificationService.sendOrderReceivedNotification(saved);

        return OrderResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        Order.OrderStatus newStatus = request.getStatus();
        Order.OrderStatus current   = order.getStatus();

        Set<Order.OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidOrderStatusTransitionException(current.name(), newStatus.name());
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        log.info("Order {} status updated: {} -> {}", id, current, newStatus);

        // Bonus: notify on completion
        if (newStatus == Order.OrderStatus.COMPLETED) {
            notificationService.sendOrderCompletedNotification(updated);
        }

        return OrderResponse.from(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUser(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        return orderRepository.findByUserId(userId, pageable)
                .map(order -> {
                    // Eagerly load items for each order in the page
                    orderRepository.findByIdWithItems(order.getId());
                    return OrderResponse.from(order);
                });
    }
}
