package org.chovy.canvas.execution.adapter.plugin.official.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.domain.DagNode;
import org.chovy.canvas.execution.domain.NodeExecutionContext;
import org.chovy.canvas.execution.domain.NodeExecutionResult;
import org.chovy.canvas.execution.domain.NodeHandler;
import org.chovy.canvas.execution.domain.NodeHandlerRegistry;
import org.junit.jupiter.api.Test;

/**
 * 定义 OfficialCouponPluginTest 的执行上下文数据结构或业务契约。
 */
class OfficialCouponPluginTest {

    /**
     * 执行 registersCouponGrantHandlerThroughExecutionRegistry 对应的业务处理。
     */
    @Test
    void registersCouponGrantHandlerThroughExecutionRegistry() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry(List.of(new OfficialCouponNodeHandler()));

        assertThat(registry.has("coupon.grant")).isTrue();
        assertThat(registry.metadata()).extracting(metadata -> metadata.nodeType())
                .containsExactly("coupon.grant");
    }

    /**
     * 执行 returnsDeterministicCouponGrantEnvelope 对应的业务处理。
     */
    @Test
    void returnsDeterministicCouponGrantEnvelope() {
        NodeHandler handler = new OfficialCouponNodeHandler();
        DagNode node = new DagNode(
                "coupon-1",
                "coupon.grant",
                "Welcome Coupon",
                Map.of("couponKey", "WELCOME_10"),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext(
                "exec-1",
                node,
                "user-1",
                Map.of("phone", "+15550001111"),
                Map.of("tenantId", "tenant-a")));

        assertThat(result.success()).isTrue();
        assertThat(result.pending()).isFalse();
        assertThat(result.error()).isEmpty();
        assertThat(result.output()).containsEntry("pluginId", "canvas-plugin-coupon")
                .containsEntry("nodeType", "coupon.grant")
                .containsEntry("couponKey", "WELCOME_10")
                .containsEntry("recipient", "user-1")
                .containsEntry("grant", "stub")
                .containsEntry("status", "SENT");
        assertThat(result.output().get("payload")).isEqualTo(Map.of("phone", "+15550001111"));
        assertThat(result.output().get("context")).isEqualTo(Map.of("tenantId", "tenant-a"));
    }

    /**
     * 执行 trimsCouponKeyAndDefaultsRecipientToAnonymousWhenUserIdIsMissing 对应的业务处理。
     */
    @Test
    void trimsCouponKeyAndDefaultsRecipientToAnonymousWhenUserIdIsMissing() {
        NodeHandler handler = new OfficialCouponNodeHandler();
        DagNode node = new DagNode(
                "coupon-1",
                "coupon.grant",
                "Welcome Coupon",
                Map.of("couponKey", " WELCOME_10 "),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("couponKey", "WELCOME_10")
                .containsEntry("recipient", "anonymous");
    }

    /**
     * 执行 failsWhenCouponKeyIsMissing 对应的业务处理。
     */
    @Test
    void failsWhenCouponKeyIsMissing() {
        NodeHandler handler = new OfficialCouponNodeHandler();
        DagNode node = new DagNode(
                "coupon-1",
                "coupon.grant",
                "Welcome Coupon",
                Map.of(),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("coupon key is required");
    }

    /**
     * 执行 failsWhenCouponKeyIsBlank 对应的业务处理。
     */
    @Test
    void failsWhenCouponKeyIsBlank() {
        NodeHandler handler = new OfficialCouponNodeHandler();
        DagNode node = new DagNode(
                "coupon-1",
                "coupon.grant",
                "Welcome Coupon",
                Map.of("couponKey", "  "),
                Map.of());

        NodeExecutionResult result = handler.execute(new NodeExecutionContext("exec-1", node, Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("coupon key is required");
    }
}
