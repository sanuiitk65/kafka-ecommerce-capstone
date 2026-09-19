package com.ecommerce.notification.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
    private String eventId;
    private String orderId;
    private String customerId;
    private String productName;
    private BigDecimal price;
    private LocalDateTime createdAt;
}
