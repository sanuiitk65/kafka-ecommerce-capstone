package com.ecommerce.payment.controller;

import com.ecommerce.payment.event.OrderCreatedEvent;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class DltReprocessingController {

    private final PaymentService paymentService;

    @PostMapping("/dlt/reprocess")
    public ResponseEntity<String> reprocessDltEvent(@RequestBody OrderCreatedEvent event) {
        log.info("REST DLT REPROCESSING TRIGGERED: Manually re-processing eventId={} for orderId={}",
                event.getEventId(), event.getOrderId());
        
        // Reset price if it was a poison pill negative price
        if (event.getPrice() != null && event.getPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            event.setPrice(java.math.BigDecimal.valueOf(99.99));
            log.info("Corrected negative price to 99.99 for reprocessing.");
        }

        paymentService.processPayment(event);
        return ResponseEntity.ok("Event reprocessed successfully!");
    }
}
