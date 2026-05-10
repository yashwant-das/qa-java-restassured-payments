package com.payflowx.backend.entity;

import com.payflowx.backend.enums.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "merchant_id")
    private String merchantId;

    @Column(name = "provider_id")
    private String providerId;

    private BigDecimal amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private Boolean subscription;
    private Integer duration;

    @Column(name = "is_renewable")
    private Boolean renewable;

    @Column(name = "receipt_hash")
    private String receiptHash;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
