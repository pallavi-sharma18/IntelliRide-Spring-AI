package com.flourish.intelliride.entities;

import com.flourish.intelliride.entities.enums.TransactionMethod;
import com.flourish.intelliride.entities.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    private TransactionType transactionType;

    private TransactionMethod transactionMethod;

    @ManyToOne
    private Ride ride;

    // Idempotency key for money movements. UNIQUE so the same logical transaction
    // (e.g. "ride:42:rider-debit") can be inserted at most once — the database is the
    // final backstop against a double debit/credit.
    @Column(unique = true)
    private String transactionId;

    @ManyToOne
    private Wallet wallet;

    @CreationTimestamp
    private LocalDateTime timeStamp;

}
