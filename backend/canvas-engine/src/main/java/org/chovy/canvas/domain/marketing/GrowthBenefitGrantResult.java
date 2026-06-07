package org.chovy.canvas.domain.marketing;

import java.util.Map;

public record GrowthBenefitGrantResult(
        Long grantId,
        String status,
        Map<String, Object> providerResponse) {
}
