package com.payflowx.backend.entity;

import com.payflowx.backend.enums.ProviderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "providers")
public class ProviderEntity {
    @Id
    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "provider_type")
    private String providerType;

    @Enumerated(EnumType.STRING)
    private ProviderStatus status;
}
