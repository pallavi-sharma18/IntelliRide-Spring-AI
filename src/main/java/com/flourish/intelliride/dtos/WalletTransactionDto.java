package com.flourish.intelliride.dtos;

import com.flourish.intelliride.entities.enums.TransactionMethod;
import com.flourish.intelliride.entities.enums.TransactionType;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class WalletTransactionDto {

    private Long id;

    private Double amount;

    private TransactionType transactionType;

    private TransactionMethod transactionMethod;

    private RideDto ride;

    private String transactionId;

    private WalletDto wallet;

    private LocalDateTime timeStamp;
}
