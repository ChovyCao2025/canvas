package org.chovy.canvas.bi.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * BiPermissionPolicyTest 测试类。
 */
class BiPermissionPolicyTest {
    /**
     * policy 字段值。
     */
    private final BiPermissionPolicy policy = new BiPermissionPolicy();

    /**
     * 执行 explicit User Deny Wins Over Role And All Allows 相关处理。
     */
    @Test
    void explicitUserDenyWinsOverRoleAndAllAllows() {
        BiAccessDecision decision = policy.evaluate(new BiAccessRequest(
                7L,
                10L,
                "dashboard",
                100L,
                "alice",
                Set.of("analyst"),
                "view"), List.of(
                grant("ALL", "*", "VIEW", "ALLOW"),
                grant("ROLE", "analyst", "VIEW", "ALLOW"),
                grant("USER", "alice", "VIEW", "DENY")));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.effect()).isEqualTo("DENY");
        assertThat(decision.matchedSubjectType()).isEqualTo("USER");
        assertThat(decision.signature()).isEqualTo("bi-permission:v1:7:10:DASHBOARD:100:VIEW:DENY:USER:alice");
    }
    /**
     * 生成默认值。
     */
    @Test
    void defaultsToDenyWhenNoGrantMatchesRequestedAction() {
        BiAccessDecision decision = policy.evaluate(new BiAccessRequest(
                7L,
                10L,
                "dataset",
                200L,
                "bob",
                Set.of("viewer"),
                "export"), List.of(
                grant("ROLE", "viewer", "VIEW", "ALLOW")));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).contains("no BI permission grant matched");
    }
    /**
     * 执行 grant 相关处理。
     */
    private static BiPermissionGrant grant(String subjectType, String subjectId, String action, String effect) {
        return new BiPermissionGrant(
                1L,
                7L,
                10L,
                "DASHBOARD",
                100L,
                subjectType,
                subjectId,
                action,
                effect,
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"));
    }
}
