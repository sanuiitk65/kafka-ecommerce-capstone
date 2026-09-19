package com.ecommerce.order.service;

import tools.jackson.databind.ObjectMapper;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.entity.OutboxStatus;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        String eventId = UUID.randomUUID().toString();

        // 1. Save Order Entity
        Order order = Order.builder()
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .productName(request.getProductName())
                .price(request.getPrice())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        orderRepository.save(order);

        // 2. Prepare Event Payload
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .productName(request.getProductName())
                .price(request.getPrice())
                .createdAt(order.getCreatedAt())
                .build();

        try {
            String jsonPayload = objectMapper.writeValueAsString(event);

            // 3. Save Outbox Record (Same Transaction as Order Entity!)
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)
                    .aggregateType("ORDER")
                    .aggregateId(orderId)
                    .eventType("OrderCreatedEvent")
                    .payload(jsonPayload)
                    .status(OutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxEventRepository.save(outboxEvent);

            log.info("Saved order {} and Outbox event {} atomically in local DB transaction.", orderId, eventId);
        } catch (Exception e) {
            log.error("Failed to serialize OrderCreatedEvent for orderId: {}", orderId, e);
            throw new RuntimeException("Order creation failed due to serialization error", e);
        }

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .productName(order.getProductName())
                .price(order.getPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
