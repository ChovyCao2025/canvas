package org.chovy.canvas.domain.risk.governance;

import org.chovy.canvas.domain.risk.dsl.RiskSubjectType;

import java.time.Instant;

/**
 * 风控名单条目写入命令。
 *
 * @param rawSubject 原始主体标识
 * @param subjectType 主体类型
 * @param reason 入名单原因
 * @param source 条目来源
 * @param effectiveFrom 生效时间
 * @param expiresAt 过期时间
 */
public record RiskListEntryCommand(
        String rawSubject,
        RiskSubjectType subjectType,
        String reason,
        String source,
        Instant effectiveFrom,
        Instant expiresAt
) {

    /**
     * 返回替换主体类型后的命令副本。
     */
    public RiskListEntryCommand withSubjectType(RiskSubjectType newSubjectType) {
        return new RiskListEntryCommand(rawSubject, newSubjectType, reason, source, effectiveFrom, expiresAt);
    }

    /**
     * 返回替换生效窗口后的命令副本。
     */
    public RiskListEntryCommand withWindow(Instant newEffectiveFrom, Instant newExpiresAt) {
        return new RiskListEntryCommand(rawSubject, subjectType, reason, source, newEffectiveFrom, newExpiresAt);
    }
}
