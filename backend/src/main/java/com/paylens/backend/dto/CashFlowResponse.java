package com.paylens.backend.dto;

import java.util.List;

public record CashFlowResponse(
        String period,
        List<CashFlowPoint> points,
        String dataQualityMessage
) {}
