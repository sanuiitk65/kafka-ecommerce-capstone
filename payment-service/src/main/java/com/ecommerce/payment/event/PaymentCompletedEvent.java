package com.ecommerce.payment.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {
    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime completedAt;
}
