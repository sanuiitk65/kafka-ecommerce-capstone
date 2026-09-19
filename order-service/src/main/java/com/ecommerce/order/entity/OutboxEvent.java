package com.ecommerce.order.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String aggregateType; // e.g. "ORDER"

    @Column(nullable = false)
    private String aggregateId;   // e.g. orderId

    @Column(nullable = false)
    private String eventType;     // e.g. "OrderCreatedEvent"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;       // JSON string

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    private LocalDateTime createdAt;
}
