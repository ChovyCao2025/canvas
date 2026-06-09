package org.chovy.canvas.domain.risk.governance;

/**
 * 风控场景视图，描述在线决策入口的租户、状态、默认模式和延迟预算。
 *
 * @param tenantId 场景所属租户
 * @param sceneKey 场景业务键
 * @param displayName 场景展示名称
 * @param eventSchemaKey 事件结构定义键
 * @param status 场景状态
 * @param defaultMode 默认运行模式
 * @param failPolicy 默认失败策略
 * @param latencyBudgetMs 在线决策延迟预算
 * @param owner 维护方
 */
public record RiskSceneView(
        Long tenantId,
        String sceneKey,
        String displayName,
        String eventSchemaKey,
        String status,
        String defaultMode,
        String failPolicy,
        Integer latencyBudgetMs,
        String owner) {
}
