package com.flourish.intelliride.services;

import com.flourish.intelliride.entities.Payment;
import com.flourish.intelliride.entities.Ride;
import com.flourish.intelliride.entities.enums.PaymentStatus;

public interface PaymentService {
    void processPayment(Ride ride);
    Payment createNewPayment(Ride ride);
    void updatePaymentStatus(Payment payment, PaymentStatus status);

}
