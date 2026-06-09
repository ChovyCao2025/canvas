package org.chovy.canvas.domain.risk.feature;

/**
 * 风控特征基础定义。
 *
 * @param featureKey 特征业务键
 * @param valueType 特征值类型
 * @param subjectField 特征关联的主体字段
 */
public record RiskFeatureDefinition(
        String featureKey,
        String valueType,
        String subjectField
) {
}
