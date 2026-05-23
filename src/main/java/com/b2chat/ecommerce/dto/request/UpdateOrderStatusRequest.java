package com.b2chat.ecommerce.dto.request;

import com.b2chat.ecommerce.entity.Order;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private Order.OrderStatus status;
}
