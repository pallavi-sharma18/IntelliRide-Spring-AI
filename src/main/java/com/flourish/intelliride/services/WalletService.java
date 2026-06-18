package com.flourish.intelliride.services;

import com.flourish.intelliride.entities.Ride;
import com.flourish.intelliride.entities.User;
import com.flourish.intelliride.entities.Wallet;
import com.flourish.intelliride.entities.enums.TransactionMethod;

public interface WalletService {
    Wallet addMoneyToWallet(User user, Double amount, String transactionId, Ride ride, TransactionMethod transactionMethod);
    Wallet deductMoneyFromWallet(User user, Double amount, String transactionId, Ride ride, TransactionMethod transactionMethod);
    void withdrawAllMyMoneyFromWallet();
    Wallet findWalletById(Long walletId);
    Wallet createNewWallet(User user);
    Wallet findByUser(User user);
}
