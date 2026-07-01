package com.flourish.intelliride.strategies.impl;

import com.flourish.intelliride.entities.Driver;
import com.flourish.intelliride.entities.Payment;
import com.flourish.intelliride.entities.Rider;
import com.flourish.intelliride.entities.enums.PaymentStatus;
import com.flourish.intelliride.entities.enums.TransactionMethod;
import com.flourish.intelliride.repositories.PaymentRepository;
import com.flourish.intelliride.services.WalletService;
import com.flourish.intelliride.strategies.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletPaymentStrategy implements PaymentStrategy {
    private final WalletService walletService;
    private final PaymentRepository paymentRepository;
    @Override
    @Transactional
    public void processPayment(Payment payment) {

        Driver driver = payment.getRide().getDriver();
        Rider rider = payment.getRide().getRider();
        Long rideId = payment.getRide().getId();

        // Deterministic idempotency keys per logical movement. Combined with the UNIQUE
        // constraint on wallet_transaction.transaction_id, a duplicate settlement of this
        // ride can never persist a second ledger row.
        // deduct money from rider wallet
        walletService.deductMoneyFromWallet(rider.getUser(), payment.getAmount(),
                "ride:" + rideId + ":rider-debit",
                payment.getRide(), TransactionMethod.RIDE);

        double driversCut = payment.getAmount()*(1-PLATFORM_COMMISSION);

        // add money to drivers wallet after removing the uber commission
        walletService.addMoneyToWallet(driver.getUser(), driversCut,
                "ride:" + rideId + ":driver-credit",
                payment.getRide(), TransactionMethod.RIDE);

        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
        paymentRepository.save(payment);
    }
}
