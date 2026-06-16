package org.chovy.canvas.bi.domain;
/**
 * BiAccessDecision 不可变数据载体。
 */
public record BiAccessDecision(
        /**
         * allowed 字段值。
         */
        boolean allowed,
        /**
         * effect 字段值。
         */
        String effect,
        /**
         * matchedSubjectType 字段值。
         */
        String matchedSubjectType,
        /**
         * matchedSubjectId 对应的标识。
         */
        String matchedSubjectId,
        /**
         * reason 字段值。
         */
        String reason,
        /**
         * 权限决策签名。
         */
        String signature
) {
}
