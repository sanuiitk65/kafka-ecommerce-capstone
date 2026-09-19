package com.ecommerce.payment.exception;

public class TransientPaymentException extends RuntimeException {
    public TransientPaymentException(String message) {
        super(message);
    }
}
