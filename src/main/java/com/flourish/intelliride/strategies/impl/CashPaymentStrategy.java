package com.flourish.intelliride.strategies.impl;

import com.flourish.intelliride.entities.Driver;
import com.flourish.intelliride.entities.Payment;
import com.flourish.intelliride.entities.enums.PaymentStatus;
import com.flourish.intelliride.entities.enums.TransactionMethod;
import com.flourish.intelliride.repositories.PaymentRepository;
import com.flourish.intelliride.services.WalletService;
import com.flourish.intelliride.strategies.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashPaymentStrategy implements PaymentStrategy {
    private final WalletService walletService;
    private final PaymentRepository paymentRepository;
    @Override
    public void processPayment(Payment payment) {
        Driver driver = payment.getRide().getDriver();
        double platformCommission = payment.getAmount()*0.3;

        walletService.deductMoneyFromWallet(driver.getUser(),platformCommission,null,
                payment.getRide(), TransactionMethod.RIDE);

        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
        paymentRepository.save(payment);
    }
}
