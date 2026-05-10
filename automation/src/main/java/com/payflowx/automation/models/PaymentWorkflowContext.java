package com.payflowx.automation.models;

import lombok.Data;

@Data
public class PaymentWorkflowContext {
    private String merchantId;
    private String providerId;
    private String transactionId;
    private String requestId;
    private String redirectUrl;
    private String providerHash;
    private String receipt;
}
