package com.anysale.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "billing_webhook_event")
public class BillingWebhookEvent {
    @Id private UUID id;
    @Column(nullable = false, length = 32) private String provider;
    @Column(name = "provider_event_id", nullable = false, length = 128) private String providerEventId;
    @Column(name = "event_type", nullable = false, length = 96) private String eventType;
    @Column(nullable = false, columnDefinition = "jsonb") private String payload;
    @Column(name = "processing_result", nullable = false, length = 32) private String processingResult;
    @Column(name = "received_at", nullable = false) private Instant receivedAt;
    @Column(name = "processed_at") private Instant processedAt;
    @PrePersist void created() { if(id==null) id=UUID.randomUUID(); if(receivedAt==null) receivedAt=Instant.now(); }
    public void setProvider(String value){provider=value;} public void setProviderEventId(String value){providerEventId=value;}
    public void setEventType(String value){eventType=value;} public void setPayload(String value){payload=value;}
    public void setProcessingResult(String value){processingResult=value;} public String getProcessingResult(){return processingResult;}
    public void setProcessedAt(Instant value){processedAt=value;}
}
