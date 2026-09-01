package com.anysale.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "billing_checkout")
public class BillingCheckout {
    @Id private UUID id;
    @Column(name="tenant_id",nullable=false,length=64) private String tenantId;
    @Column(name="plan_code",nullable=false,length=64) private String planCode;
    @Column(nullable=false,length=32) private String provider;
    @Column(name="provider_checkout_id",nullable=false,length=128) private String providerCheckoutId;
    @Column(name="external_reference",nullable=false,length=200) private String externalReference;
    @Column(name="checkout_url",nullable=false,length=1000) private String checkoutUrl;
    @Column(nullable=false,length=32) private String status;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @PrePersist void created(){if(id==null)id=UUID.randomUUID(); if(createdAt==null)createdAt=Instant.now();}
    public String getTenantId(){return tenantId;} public void setTenantId(String value){tenantId=value;}
    public String getPlanCode(){return planCode;} public void setPlanCode(String value){planCode=value;}
    public String getProvider(){return provider;} public void setProvider(String value){provider=value;}
    public String getProviderCheckoutId(){return providerCheckoutId;} public void setProviderCheckoutId(String value){providerCheckoutId=value;}
    public String getExternalReference(){return externalReference;} public void setExternalReference(String value){externalReference=value;}
    public String getCheckoutUrl(){return checkoutUrl;} public void setCheckoutUrl(String value){checkoutUrl=value;}
    public String getStatus(){return status;} public void setStatus(String value){status=value;}
    public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant value){expiresAt=value;}
}
