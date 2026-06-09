package org.chovy.canvas.domain.marketing;

import java.util.Map;

/**
 * GrowthBenefitGrantResult 承载 domain.marketing 场景中的不可变数据快照。
 * @param grantId grantId 字段。
 * @param status status 字段。
 * @param providerResponse providerResponse 字段。
 */
public record GrowthBenefitGrantResult(
        Long grantId,
        String status,
        Map<String, Object> providerResponse) {
}
