package org.chovy.canvas.domain.risk.dsl;

/**
 * 风控名单定义。
 *
 * @param key 名单业务键
 * @param subjectType 名单主体类型
 * @param valueType 名单值类型
 */
public record RiskListDefinition(
        String key,
        RiskSubjectType subjectType,
        RiskValueType valueType
) {
}
