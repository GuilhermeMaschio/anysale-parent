package com.anysale.billing.persistence;

import com.anysale.billing.domain.BillingPlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingPlanRepository extends JpaRepository<BillingPlan, String> {
    List<BillingPlan> findByActiveTrueOrderByMonthlyPriceCentsAsc();
}
