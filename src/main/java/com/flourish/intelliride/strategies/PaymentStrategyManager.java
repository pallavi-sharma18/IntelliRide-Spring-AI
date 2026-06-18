package com.flourish.intelliride.strategies;

import com.flourish.intelliride.entities.enums.PaymentMethod;
import com.flourish.intelliride.strategies.impl.CashPaymentStrategy;
import com.flourish.intelliride.strategies.impl.WalletPaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentStrategyManager {

    private final WalletPaymentStrategy walletPaymentStrategy;
    private final CashPaymentStrategy cashPaymentStrategy;

    public PaymentStrategy paymentStrategy(PaymentMethod paymentMethod){

        return switch(paymentMethod){
            case WALLET -> walletPaymentStrategy;
            case CASH -> cashPaymentStrategy;
        };

    }
}
