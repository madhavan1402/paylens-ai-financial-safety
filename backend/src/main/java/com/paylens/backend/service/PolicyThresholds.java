package com.paylens.backend.service;

import com.paylens.backend.dto.FinancialSnapshot;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Central location for Phase 3 policy thresholds. */
@Component
public class PolicyThresholds {
    /** The required margin is one full safety reserve in this phase. */
    public BigDecimal minimumSafetyMargin(FinancialSnapshot snapshot) {
        return snapshot.safetyReserve();
    }
}
