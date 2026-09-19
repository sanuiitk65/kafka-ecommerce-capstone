package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.ProcessedEvent;
import com.ecommerce.payment.event.OrderCreatedEvent;
import com.ecommerce.payment.event.PaymentCompletedEvent;
import com.ecommerce.payment.exception.TransientPaymentException;
import com.ecommerce.payment.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PAYMENTS_TOPIC = "payments";

    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        // 1. Application-Level Idempotency Check (Phase 6)
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.warn("IDEMPOTENCY TRIGGERED: EventId {} for OrderId {} has already been processed. Dropping duplicate event!",
                    event.getEventId(), event.getOrderId());
            return;
        }

        log.info("Processing payment for OrderId: {} | CustomerId: {} | Price: {}",
                event.getOrderId(), event.getCustomerId(), event.getPrice());

        // 2. Simulate Poison Pill Exception (Negative Price -> Non-Retryable -> Sent to DLT)
        if (event.getPrice() != null && event.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            log.error("POISON PILL DETECTED: Invalid negative price {} for OrderId: {}. Throwing IllegalArgumentException!",
                    event.getPrice(), event.getOrderId());
            throw new IllegalArgumentException("Negative order price is not allowed: " + event.getPrice());
        }

        // 3. Simulate Transient Gateway Timeout Exception (Triggers @RetryableTopic Retries!)
        if ("FAIL_PAYMENT".equalsIgnoreCase(event.getProductName())) {
            log.warn("TRANSIENT FAILURE DETECTED: Simulated payment gateway timeout for OrderId: {}. Throwing TransientPaymentException!",
                    event.getOrderId());
            throw new TransientPaymentException("Payment Gateway Timeout for order: " + event.getOrderId());
        }

        // 4. Save Processed Event to DB for Idempotency tracking
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(event.getEventId())
                .orderId(event.getOrderId())
                .processedAt(LocalDateTime.now())
                .build();
        processedEventRepository.save(processedEvent);

        // 5. Emit Downstream PaymentCompletedEvent
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
        PaymentCompletedEvent paymentEvent = PaymentCompletedEvent.builder()
                .paymentId(paymentId)
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .amount(event.getPrice())
                .status("COMPLETED")
                .completedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(PAYMENTS_TOPIC, event.getCustomerId(), paymentEvent);
        log.info("Payment SUCCESS: Emitted PaymentCompletedEvent {} to topic {}.", paymentId, PAYMENTS_TOPIC);
    }
}
