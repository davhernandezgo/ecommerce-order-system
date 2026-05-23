package com.b2chat.ecommerce.service.async;

import com.b2chat.ecommerce.entity.Order;
import com.b2chat.ecommerce.entity.OrderItem;
import com.b2chat.ecommerce.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Email notification service.
 * Both methods are marked @Async so they run on the order-async thread pool
 * without blocking the HTTP response.
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@local.dev}")
    private String fromAddress;

    @Value("${app.notification.enabled:true}")
    private boolean notificationsEnabled;

    @Override
    @Async("orderTaskExecutor")
    public void sendOrderReceivedNotification(Order order) {
        if (!notificationsEnabled) {
            log.info("[NOTIFICATION-STUB] Order {} received — notification skipped (disabled).", order.getId());
            return;
        }
        try {
            String subject = "Order #" + order.getId() + " Received – B2Chat E-Commerce";
            String body    = buildOrderReceivedBody(order);
            sendEmail(order.getUser().getEmail(), subject, body);
            log.info("Order-received email sent to {} for order #{}", order.getUser().getEmail(), order.getId());
        } catch (Exception ex) {
            log.error("Failed to send order-received notification for order #{}: {}",
                    order.getId(), ex.getMessage());
        }
    }

    @Override
    @Async("orderTaskExecutor")
    public void sendOrderCompletedNotification(Order order) {
        if (!notificationsEnabled) {
            log.info("[NOTIFICATION-STUB] Order {} completed — notification skipped (disabled).", order.getId());
            return;
        }
        try {
            String subject = "Order #" + order.getId() + " Completed – B2Chat E-Commerce";
            String body    = buildOrderCompletedBody(order);
            sendEmail(order.getUser().getEmail(), subject, body);
            log.info("Order-completed email sent to {} for order #{}", order.getUser().getEmail(), order.getId());
        } catch (Exception ex) {
            log.error("Failed to send order-completed notification for order #{}: {}",
                    order.getId(), ex.getMessage());
        }
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private String buildOrderReceivedBody(Order order) {
        String itemLines = order.getItems().stream()
                .map(item -> String.format("  - %s x%d @ $%.2f = $%.2f",
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .collect(Collectors.joining("\n"));

        return String.format(
                "Hello %s,\n\n" +
                "We have received your order #%d.\n\n" +
                "Items:\n%s\n\n" +
                "Total: $%.2f\n\n" +
                "Status: %s\n\n" +
                "Thank you for shopping with B2Chat E-Commerce!",
                order.getUser().getName(),
                order.getId(),
                itemLines,
                order.getTotalAmount(),
                order.getStatus().name()
        );
    }

    private String buildOrderCompletedBody(Order order) {
        return String.format(
                "Hello %s,\n\n" +
                "Great news! Your order #%d has been completed and is on its way.\n\n" +
                "Total charged: $%.2f\n\n" +
                "Thank you for shopping with B2Chat E-Commerce!",
                order.getUser().getName(),
                order.getId(),
                order.getTotalAmount()
        );
    }
}
