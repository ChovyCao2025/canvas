package org.chovy.canvas.bi.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
/**
 * BiDashboardWidgetDO 持久化对象。
 */
@TableName("bi_dashboard_widget")
public class BiDashboardWidgetDO {
    /**
     * 唯一标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户标识。
     */
    private Long tenantId;

    /**
     * dashboardId 对应的标识。
     */
    private Long dashboardId;

    /**
     * widgetKey 对应的业务键。
     */
    private String widgetKey;

    /**
     * chartId 对应的标识。
     */
    private Long chartId;

    /**
     * widgetType 字段值。
     */
    private String widgetType;

    /**
     * 展示标题。
     */
    private String title;

    /**
     * layoutJson 的 JSON 序列化内容。
     */
    private String layoutJson;

    /**
     * queryOverrideJson 的 JSON 序列化内容。
     */
    private String queryOverrideJson;

    /**
     * interactionJson 的 JSON 序列化内容。
     */
    private String interactionJson;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 获取 Id。
     */
    public Long getId() {
        return id;
    }
    /**
     * 设置 Id。
     */
    public void setId(Long id) {
        this.id = id;
    }
    /**
     * 获取 Tenant Id。
     */
    public Long getTenantId() {
        return tenantId;
    }
    /**
     * 设置 Tenant Id。
     */
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
    /**
     * 获取 Dashboard Id。
     */
    public Long getDashboardId() {
        return dashboardId;
    }
    /**
     * 设置 Dashboard Id。
     */
    public void setDashboardId(Long dashboardId) {
        this.dashboardId = dashboardId;
    }
    /**
     * 获取 Widget Key。
     */
    public String getWidgetKey() {
        return widgetKey;
    }
    /**
     * 设置 Widget Key。
     */
    public void setWidgetKey(String widgetKey) {
        this.widgetKey = widgetKey;
    }
    /**
     * 获取 Chart Id。
     */
    public Long getChartId() {
        return chartId;
    }
    /**
     * 设置 Chart Id。
     */
    public void setChartId(Long chartId) {
        this.chartId = chartId;
    }
    /**
     * 获取 Widget Type。
     */
    public String getWidgetType() {
        return widgetType;
    }
    /**
     * 设置 Widget Type。
     */
    public void setWidgetType(String widgetType) {
        this.widgetType = widgetType;
    }
    /**
     * 获取 Title。
     */
    public String getTitle() {
        return title;
    }
    /**
     * 设置 Title。
     */
    public void setTitle(String title) {
        this.title = title;
    }
    /**
     * 获取 Layout Json。
     */
    public String getLayoutJson() {
        return layoutJson;
    }
    /**
     * 设置 Layout Json。
     */
    public void setLayoutJson(String layoutJson) {
        this.layoutJson = layoutJson;
    }
    /**
     * 获取 Query Override Json。
     */
    public String getQueryOverrideJson() {
        return queryOverrideJson;
    }
    /**
     * 设置 Query Override Json。
     */
    public void setQueryOverrideJson(String queryOverrideJson) {
        this.queryOverrideJson = queryOverrideJson;
    }
    /**
     * 获取 Interaction Json。
     */
    public String getInteractionJson() {
        return interactionJson;
    }
    /**
     * 设置 Interaction Json。
     */
    public void setInteractionJson(String interactionJson) {
        this.interactionJson = interactionJson;
    }
    /**
     * 获取 Created At。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    /**
     * 设置 Created At。
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    /**
     * 获取 Updated At。
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    /**
     * 设置 Updated At。
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
