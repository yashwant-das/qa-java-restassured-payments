package com.payflowx.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "merchants")
public class MerchantEntity {
    @Id
    @Column(name = "merchant_id")
    private String merchantId;

    @Column(name = "merchant_name")
    private String merchantName;

    private String status;

    @Column(name = "created_at")
    private Instant createdAt;
}
