package org.chovy.canvas.risk.domain.dsl;

/**
 * 风控特征定义。
 *
 * @param key 特征业务键
 * @param valueType 特征值类型
 * @param availability 特征在线可用性
 * @param subjectType 特征关联的主体类型
 */
public record RiskFactorDefinition(
        String key,
        RiskValueType valueType,
        RiskFeatureAvailability availability,
        RiskSubjectType subjectType
) {
}
