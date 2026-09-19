package com.ecommerce.order.repository;

import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
