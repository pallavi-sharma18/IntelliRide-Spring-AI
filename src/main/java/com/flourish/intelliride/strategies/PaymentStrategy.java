package com.flourish.intelliride.strategies;

import com.flourish.intelliride.entities.Payment;

public interface PaymentStrategy {

    Double PLATFORM_COMMISSION = 0.3;
    void processPayment(Payment payment);
}
