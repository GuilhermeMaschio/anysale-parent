package com.anysale.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name = "billing_plan")
public class BillingPlan {
    @Id @Column(length = 64) private String code;
    @Column(nullable = false, length = 80) private String name;
    @Column(nullable = false, length = 300) private String description;
    @Column(name = "monthly_price_cents") private Integer monthlyPriceCents;
    @Column(name = "user_limit") private Integer userLimit;
    @Column(name = "monthly_lead_limit") private Integer monthlyLeadLimit;
    @Column(name = "monthly_ai_request_limit") private Integer monthlyAiRequestLimit;
    @Column(name = "trial_days", nullable = false) private int trialDays;
    @Column(name = "grace_days", nullable = false) private int graceDays;
    @Column(nullable = false) private boolean active;
    public String getCode(){return code;} public String getName(){return name;} public String getDescription(){return description;}
    public Integer getMonthlyPriceCents(){return monthlyPriceCents;} public Integer getUserLimit(){return userLimit;}
    public Integer getMonthlyLeadLimit(){return monthlyLeadLimit;} public Integer getMonthlyAiRequestLimit(){return monthlyAiRequestLimit;}
    public int getTrialDays(){return trialDays;} public int getGraceDays(){return graceDays;} public boolean isActive(){return active;}
}
