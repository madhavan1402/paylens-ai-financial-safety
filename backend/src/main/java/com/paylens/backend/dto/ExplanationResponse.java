package com.paylens.backend.dto;

import com.paylens.backend.model.PolicyDecision;
import java.util.List;

public record ExplanationResponse(
        String status,
        PolicyDecision decision,
        String headline,
        String explanation,
        List<String> keyFactors,
        String recommendation,
        String providerMode) {
}
