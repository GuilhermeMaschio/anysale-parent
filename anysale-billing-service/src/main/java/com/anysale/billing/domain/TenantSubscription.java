package com.anysale.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity @Table(name = "billing_subscription")
public class TenantSubscription {
    @Id @Column(name = "tenant_id", length = 64) private String tenantId;
    @Column(nullable = false, length = 32) private String provider;
    @Column(name = "provider_customer_id", length = 128) private String providerCustomerId;
    @Column(name = "provider_subscription_id", length = 128) private String providerSubscriptionId;
    @Column(name = "plan_code", nullable = false, length = 64) private String planCode;
    @Column(nullable = false, length = 32) private String status;
    @Column(name = "trial_ends_at") private Instant trialEndsAt;
    @Column(name = "current_period_ends_at") private Instant currentPeriodEndsAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void created() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void updated() { updatedAt = Instant.now(); }
    public String getTenantId(){return tenantId;} public void setTenantId(String value){tenantId=value;}
    public String getProvider(){return provider;} public void setProvider(String value){provider=value;}
    public String getProviderCustomerId(){return providerCustomerId;} public void setProviderCustomerId(String value){providerCustomerId=value;}
    public String getProviderSubscriptionId(){return providerSubscriptionId;} public void setProviderSubscriptionId(String value){providerSubscriptionId=value;}
    public String getPlanCode(){return planCode;} public void setPlanCode(String value){planCode=value;}
    public String getStatus(){return status;} public void setStatus(String value){status=value;}
    public Instant getTrialEndsAt(){return trialEndsAt;} public void setTrialEndsAt(Instant value){trialEndsAt=value;}
    public Instant getCurrentPeriodEndsAt(){return currentPeriodEndsAt;} public void setCurrentPeriodEndsAt(Instant value){currentPeriodEndsAt=value;}
}
