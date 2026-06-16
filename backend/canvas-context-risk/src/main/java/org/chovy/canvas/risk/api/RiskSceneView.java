package org.chovy.canvas.risk.api;

import java.util.Objects;

/**
 * 定义 RiskSceneView 的风控模块职责和数据契约。
 */
public final class RiskSceneView {

    /**
     * RiskSceneView 的 tenantId 字段。
     */
    private final Long tenantId;


    /**
     * RiskSceneView 的 sceneKey 字段。
     */
    private final String sceneKey;


    /**
     * RiskSceneView 的 displayName 字段。
     */
    private final String displayName;


    /**
     * RiskSceneView 的 eventSchemaKey 字段。
     */
    private final String eventSchemaKey;


    /**
     * RiskSceneView 的 status 字段。
     */
    private final String status;


    /**
     * RiskSceneView 的 defaultMode 字段。
     */
    private final String defaultMode;


    /**
     * RiskSceneView 的 failPolicy 字段。
     */
    private final String failPolicy;


    /**
     * RiskSceneView 的 latencyBudgetMs 字段。
     */
    private final Integer latencyBudgetMs;


    /**
     * RiskSceneView 的 owner 字段。
     */
    private final String owner;


    /**
     * 创建 RiskSceneView。
     *
     * @param tenantId RiskSceneView 的 tenantId 字段
     * @param sceneKey RiskSceneView 的 sceneKey 字段
     * @param displayName RiskSceneView 的 displayName 字段
     * @param eventSchemaKey RiskSceneView 的 eventSchemaKey 字段
     * @param status RiskSceneView 的 status 字段
     * @param defaultMode RiskSceneView 的 defaultMode 字段
     * @param failPolicy RiskSceneView 的 failPolicy 字段
     * @param latencyBudgetMs RiskSceneView 的 latencyBudgetMs 字段
     * @param owner RiskSceneView 的 owner 字段
     */
    public RiskSceneView(Long tenantId, String sceneKey, String displayName, String eventSchemaKey, String status, String defaultMode, String failPolicy, Integer latencyBudgetMs, String owner) {
        this.tenantId = tenantId;
        this.sceneKey = sceneKey;
        this.displayName = displayName;
        this.eventSchemaKey = eventSchemaKey;
        this.status = status;
        this.defaultMode = defaultMode;
        this.failPolicy = failPolicy;
        this.latencyBudgetMs = latencyBudgetMs;
        this.owner = owner;
    }

    /**
     * 返回 RiskSceneView 的 tenantId 字段。
     *
     * @return tenantId 字段值
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回 RiskSceneView 的 sceneKey 字段。
     *
     * @return sceneKey 字段值
     */
    public String sceneKey() {
        return sceneKey;
    }

    /**
     * 返回 RiskSceneView 的 displayName 字段。
     *
     * @return displayName 字段值
     */
    public String displayName() {
        return displayName;
    }

    /**
     * 返回 RiskSceneView 的 eventSchemaKey 字段。
     *
     * @return eventSchemaKey 字段值
     */
    public String eventSchemaKey() {
        return eventSchemaKey;
    }

    /**
     * 返回 RiskSceneView 的 status 字段。
     *
     * @return status 字段值
     */
    public String status() {
        return status;
    }

    /**
     * 返回 RiskSceneView 的 defaultMode 字段。
     *
     * @return defaultMode 字段值
     */
    public String defaultMode() {
        return defaultMode;
    }

    /**
     * 返回 RiskSceneView 的 failPolicy 字段。
     *
     * @return failPolicy 字段值
     */
    public String failPolicy() {
        return failPolicy;
    }

    /**
     * 返回 RiskSceneView 的 latencyBudgetMs 字段。
     *
     * @return latencyBudgetMs 字段值
     */
    public Integer latencyBudgetMs() {
        return latencyBudgetMs;
    }

    /**
     * 返回 RiskSceneView 的 owner 字段。
     *
     * @return owner 字段值
     */
    public String owner() {
        return owner;
    }

    /**
     * 比较当前 RiskSceneView 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskSceneView other)) {
            return false;
        }
        return Objects.equals(tenantId, other.tenantId)
                && Objects.equals(sceneKey, other.sceneKey)
                && Objects.equals(displayName, other.displayName)
                && Objects.equals(eventSchemaKey, other.eventSchemaKey)
                && Objects.equals(status, other.status)
                && Objects.equals(defaultMode, other.defaultMode)
                && Objects.equals(failPolicy, other.failPolicy)
                && Objects.equals(latencyBudgetMs, other.latencyBudgetMs)
                && Objects.equals(owner, other.owner);
    }

    /**
     * 计算 RiskSceneView 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(tenantId, sceneKey, displayName, eventSchemaKey, status, defaultMode, failPolicy, latencyBudgetMs, owner);
    }

    /**
     * 返回 RiskSceneView 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskSceneView[tenantId=" + tenantId + ", sceneKey=" + sceneKey + ", displayName=" + displayName + ", eventSchemaKey=" + eventSchemaKey + ", status=" + status + ", defaultMode=" + defaultMode + ", failPolicy=" + failPolicy + ", latencyBudgetMs=" + latencyBudgetMs + ", owner=" + owner + "]";
    }
}
