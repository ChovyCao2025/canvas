package org.chovy.canvas.domain.marketing;

import java.util.Map;

/**
 * GrowthLoyaltyResult 承载 domain.marketing 场景中的不可变数据快照。
 * @param status status 字段。
 * @param payload payload 字段。
 */
public record GrowthLoyaltyResult(
        String status,
        Map<String, Object> payload) {
}
