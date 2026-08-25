package com.paylens.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record ObligationsRiskResponse(
        List<ObligationRiskItem> obligations,
        BigDecimal totalUpcomingAmount
) {}
