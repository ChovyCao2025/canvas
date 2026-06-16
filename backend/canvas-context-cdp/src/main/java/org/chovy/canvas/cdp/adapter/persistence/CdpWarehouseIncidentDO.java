package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 承载 CdpWarehouseIncident 表的持久化字段。
 */
@TableName("cdp_warehouse_incident")
public class CdpWarehouseIncidentDO {

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
     * severity。
     */
    private String severity;

    /**
     * 状态。
     */
    private String status;

    /**
     * title。
     */
    private String title;

    /**
     * 描述。
     */
    private String description;

    /**
     * first Seen At。
     */
    private LocalDateTime firstSeenAt;

    /**
     * last Seen At。
     */
    private LocalDateTime lastSeenAt;

    /**
     * 返回唯一标识。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置唯一标识。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 返回租户标识。
     */
    public Long getTenantId() {
        return tenantId;
    }

    /**
     * 设置租户标识。
     */
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 返回severity。
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * 设置severity。
     */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /**
     * 返回状态。
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置状态。
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 返回title。
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置title。
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 返回描述。
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置描述。
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 返回first Seen At。
     */
    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    /**
     * 设置first Seen At。
     */
    public void setFirstSeenAt(LocalDateTime firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    /**
     * 返回last Seen At。
     */
    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    /**
     * 设置last Seen At。
     */
    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
