package com.ecommerce.order.scheduler;

import tools.jackson.databind.ObjectMapper;
import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.entity.OutboxStatus;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "orders";

    @Scheduled(fixedDelay = 3000) // Polls DB outbox every 3 seconds
    @Transactional
    public void publishPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending Outbox events to publish to Kafka topic {}.", pendingEvents.size(), TOPIC);

        for (OutboxEvent outbox : pendingEvents) {
            try {
                OrderCreatedEvent event = objectMapper.readValue(outbox.getPayload(), OrderCreatedEvent.class);

                // Send to Kafka with Partition Key = customerId (Guarantees order per customer)
                kafkaTemplate.send(TOPIC, event.getCustomerId(), event).whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published eventId: {} | customerId: {} | topic: {} | partition: {} | offset: {}",
                                event.getEventId(), event.getCustomerId(), TOPIC,
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish eventId: {} to Kafka", event.getEventId(), ex);
                    }
                });

                // Mark Outbox Event as PUBLISHED
                outbox.setStatus(OutboxStatus.PUBLISHED);
                outboxEventRepository.save(outbox);
            } catch (Exception e) {
                log.error("Error processing outbox event id: {}", outbox.getId(), e);
                outbox.setStatus(OutboxStatus.FAILED);
                outboxEventRepository.save(outbox);
            }
        }
    }
}
