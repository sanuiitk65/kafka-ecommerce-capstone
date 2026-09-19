package com.ecommerce.notification.consumer;

import com.ecommerce.notification.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationConsumer {

    // Demonstrates Pub/Sub Broadcasting (Phase 1 & 4)
    // Runs in a DIFFERENT Consumer Group ("notification-service-group")
    @KafkaListener(topics = "orders", groupId = "notification-service-group")
    public void consumeOrderCreated(OrderCreatedEvent event,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                    @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("PUB/SUB NOTIFICATION RECEIVED: Sending Order Confirmation Email to customerId={} for orderId={} | Product: {} | Partition: {} | Offset: {}",
                event.getCustomerId(), event.getOrderId(), event.getProductName(), partition, offset);
    }
}
