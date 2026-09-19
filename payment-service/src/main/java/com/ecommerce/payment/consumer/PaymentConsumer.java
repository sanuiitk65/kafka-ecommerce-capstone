package com.ecommerce.payment.consumer;

import com.ecommerce.payment.event.OrderCreatedEvent;
import com.ecommerce.payment.exception.TransientPaymentException;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final PaymentService paymentService;

    // Resilient Tiered Retries (Phase 5, 6 & 9)
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0), // Exponential Backoff: 1s -> 2s
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            include = { TransientPaymentException.class }
    )
    @KafkaListener(topics = "orders", groupId = "payment-service-group")
    public void consumeOrderCreated(OrderCreatedEvent event,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                    @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received OrderCreatedEvent: eventId={} | orderId={} | key={} | partition={} | offset={}",
                event.getEventId(), event.getOrderId(), key, partition, offset);

        paymentService.processPayment(event);
    }

    // Dead Letter Topic Handler (Captures Poison Pills & Retry Exhausted events into orders.DLT)
    @DltHandler
    public void handleDlt(OrderCreatedEvent event,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset,
                          @Header(name = "x-exception-message", required = false) String exceptionMessage) {
        log.error("DLT EVENT RECORDED: Message for orderId={} landed in DLT topic {} | Partition: {} | Offset: {} | Reason: {}",
                event.getOrderId(), topic, partition, offset, exceptionMessage);
    }
}
