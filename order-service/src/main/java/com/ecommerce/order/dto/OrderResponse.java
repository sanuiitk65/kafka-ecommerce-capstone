package com.ecommerce.order.dto;

import com.ecommerce.order.entity.OrderStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderResponse {
    private String orderId;
    private String customerId;
    private String productName;
    private BigDecimal price;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
