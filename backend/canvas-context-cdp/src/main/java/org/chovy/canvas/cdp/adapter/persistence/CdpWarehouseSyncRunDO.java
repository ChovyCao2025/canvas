package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 承载 CdpWarehouseSyncRun 表的持久化字段。
 */
@TableName("cdp_warehouse_sync_run")
public class CdpWarehouseSyncRunDO {

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
     * 状态。
     */
    private String status;

    /**
     * window Start。
     */
    private LocalDateTime windowStart;

    /**
     * window End。
     */
    private LocalDateTime windowEnd;

    /**
     * 开始时间。
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间。
     */
    private LocalDateTime finishedAt;

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
     * 返回window Start。
     */
    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    /**
     * 设置window Start。
     */
    public void setWindowStart(LocalDateTime windowStart) {
        this.windowStart = windowStart;
    }

    /**
     * 返回window End。
     */
    public LocalDateTime getWindowEnd() {
        return windowEnd;
    }

    /**
     * 设置window End。
     */
    public void setWindowEnd(LocalDateTime windowEnd) {
        this.windowEnd = windowEnd;
    }

    /**
     * 返回开始时间。
     */
    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    /**
     * 设置开始时间。
     */
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * 返回完成时间。
     */
    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    /**
     * 设置完成时间。
     */
    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
