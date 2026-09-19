# 🚀 Event-Driven E-Commerce & Order-Payment Orchestration Engine
> A production-grade event-driven microservices system built with **Spring Boot 3**, **Apache Kafka (KRaft mode)**, **PostgreSQL**, and **Docker**.

---

## 🏗️ System Microservices Architecture

```mermaid
flowchart LR
    Client["Client / Postman"] -->|1. POST /api/orders| OS["Order Service<br/>(Port 8081)"]
    
    subgraph OrderSvc["1. ORDER SERVICE (Producer)"]
        OS -->|Tx Commit| DB1[("PostgreSQL<br/>orders & outbox tables")]
        OutboxWorker["Outbox Publisher<br/>(Scheduled Task)"] -->|Reads Outbox| KT["KafkaTemplate"]
    end

    Broker["Kafka Cluster (KRaft)<br/>Topics: orders, payments, orders-dlt"]

    subgraph PaymentSvc["2. PAYMENT SERVICE (Consumer + Producer)"]
        CL1["@KafkaListener<br/>(Group: payment-group)"] --> IdemCheck{"Idempotency Check<br/>(processed_events Table)"}
        IdemCheck -->|New Event| ProcessPay["Process Payment Logic"]
        ProcessPay -->|Transient Error| Retry["@RetryableTopic<br/>(Exponential Backoff)"]
        Retry -->|Max Retries Exceeded| DLT["@DltHandler<br/>(Publish to orders.DLT)"]
        ProcessPay -->|Success| PayPub["KafkaTemplate"]
    end

    subgraph NotifSvc["3. NOTIFICATION SERVICE (Pub/Sub Consumer)"]
        CL2["@KafkaListener<br/>(Group: notification-group)"] --> Email["Send Notification Email"]
    end

    KT -->|Publish OrderCreatedEvent| Broker
    Broker -->|Consume orders| CL1
    Broker -->|Consume orders| CL2
    PayPub -->|Publish PaymentCompletedEvent| Broker
```

---

## 🛠️ Key Architectural Patterns Implemented

1. **Transactional Outbox Pattern (Phase 9):** Atomic local DB transaction saving `Order` and `OutboxEvent` to prevent Dual-Write inconsistencies.
2. **Key-Based Partitioning (Phase 2 & 7):** Hashing events by `customerId` to guarantee sequential entity processing.
3. **Application-Level Idempotent Consumer (Phase 6):** Duplicate detection via `processed_events` table in PostgreSQL.
4. **Resilient Retry & Backoff (Phase 5 & 6):** `@RetryableTopic` with Exponential Backoff ($1s \to 2s \to 4s$).
5. **Dead Letter Topic (DLT) & Reprocessing API (Phase 5, 6 & 9):** `@DltHandler` metadata preservation + REST Controller to inspect/re-process failed DLT events.
6. **Pub/Sub Broadcasting (Phase 1 & 4):** Multiple independent consumer groups (`payment-service-group` and `notification-service-group`).

---

## ⚡ Infrastructure Quickstart (`docker-compose.yml`)

```bash
# Spin up Kafka KRaft, PostgreSQL, and Kafdrop Web UI
docker-compose up -d
```
* **Kafdrop UI:** [http://localhost:9000](http://localhost:9000)
* **Kafka Broker:** `localhost:9092`
* **PostgreSQL:** `localhost:5432`
