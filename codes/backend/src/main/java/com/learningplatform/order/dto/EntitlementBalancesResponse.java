package com.learningplatform.order.dto;

public record EntitlementBalancesResponse(
        int aiQuota,
        int examQuota,
        int examOverallAiQuota,
        int examPersonalAiQuota
) {
}
