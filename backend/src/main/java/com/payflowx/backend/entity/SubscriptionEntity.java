package com.payflowx.backend.entity;

import com.payflowx.backend.enums.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity {
    @Id
    @Column(name = "subscription_id")
    private String subscriptionId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "renewal_count")
    private Integer renewalCount;

    @Column(name = "next_renewal_date")
    private LocalDate nextRenewalDate;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    @Column(name = "created_at")
    private Instant createdAt;
}
