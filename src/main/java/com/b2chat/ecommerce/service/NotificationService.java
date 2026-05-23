package com.b2chat.ecommerce.service;

import com.b2chat.ecommerce.entity.Order;

/**
 * Service for sending notifications about order events.
 */
public interface NotificationService {

    /**
     * Sends a notification confirming that an order has been received.
     * Called asynchronously after a successful order placement.
     */
    void sendOrderReceivedNotification(Order order);

    /**
     * Sends a notification when an order status changes to COMPLETED.
     */
    void sendOrderCompletedNotification(Order order);
}
