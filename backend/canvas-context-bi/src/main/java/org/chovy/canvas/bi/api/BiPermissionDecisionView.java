package org.chovy.canvas.bi.api;
/**
 * BiPermissionDecisionView 视图。
 */
public record BiPermissionDecisionView(
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
