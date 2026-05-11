package com.photon.canvas.engine.handlers;

import com.photon.canvas.engine.context.ExecutionContext;
import com.photon.canvas.engine.context.NodeStatus;
import com.photon.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * LogicRelationHandler 单元测试。
 * 覆盖：AND 所有上游 SUCCESS、OR 任意 SUCCESS、AND 遇 FAILED 立即失败、SKIPPED 传播。
 */
class LogicRelationHandlerTest {

    @Nested
    @DisplayName("AND 模式")
    class AndMode {

        @Test
        @DisplayName("所有上游 SUCCESS → 条件满足")
        void all_success_met() {
            ExecutionContext ctx = buildCtx("n1", NodeStatus.SUCCESS, "n2", NodeStatus.SUCCESS);
            assertThat(LogicRelationHandler.checkCondition("AND", List.of("n1", "n2"), ctx)).isTrue();
        }

        @Test
        @DisplayName("有上游 PENDING → 条件未满足（等待）")
        void pending_upstream_not_met() {
            ExecutionContext ctx = buildCtx("n1", NodeStatus.SUCCESS, "n2", NodeStatus.PENDING);
            assertThat(LogicRelationHandler.checkCondition("AND", List.of("n1", "n2"), ctx)).isFalse();
        }

        @Test
        @DisplayName("有上游 FAILED → shouldFailImmediately 为 true")
        void failed_upstream_immediate_fail() {
            ExecutionContext ctx = buildCtx("n1", NodeStatus.SUCCESS, "n2", NodeStatus.FAILED);
            assertThat(LogicRelationHandler.shouldFailImmediately("AND", List.of("n1", "n2"), ctx)).isTrue();
        }

        @Test
        @DisplayName("有上游 SKIPPED → shouldFailImmediately 为 true")
        void skipped_upstream_blocks_and() {
            ExecutionContext ctx = buildCtx("n1", NodeStatus.SUCCESS, "n2", NodeStatus.SKIPPED);
            assertThat(LogicRelationHandler.shouldFailImmediately("AND", List.of("n1", "n2"), ctx)).isTrue();
        }
    }

    @Nested
    @DisplayName("OR 模式")
    class OrMode {

        @Test
        @DisplayName("任意一个上游 SUCCESS → 条件满足")
        void one_success_met() {
            ExecutionContext ctx = buildCtx("n1", NodeStatus.SUCCESS, "n2", NodeStatus.PENDING);
            assertThat(LogicRelationHandler.checkCondition("OR", List.of("n1", "n2"), ctx)).isTrue();
        }

        @Test
        @DisplayName("所有上游 PENDING → 条件未满足")
        void all_pending_not_met() {
            ExecutionContext ctx = buildCtx("n1", NodeStatus.PENDING, "n2", NodeStatus.PENDING);
            assertThat(LogicRelationHandler.checkCondition("OR", List.of("n1", "n2"), ctx)).isFalse();
        }

        @Test
        @DisplayName("OR 模式：上游 FAILED 不触发立即失败")
        void failed_in_or_not_immediate_fail() {
            ExecutionContext ctx = buildCtx("n1", NodeStatus.FAILED, "n2", NodeStatus.PENDING);
            assertThat(LogicRelationHandler.shouldFailImmediately("OR", List.of("n1", "n2"), ctx)).isFalse();
        }
    }

    private ExecutionContext buildCtx(Object... nodeStatuses) {
        ExecutionContext ctx = new ExecutionContext();
        for (int i = 0; i < nodeStatuses.length; i += 2) {
            ctx.setNodeStatus((String) nodeStatuses[i], (NodeStatus) nodeStatuses[i + 1]);
        }
        return ctx;
    }
}
