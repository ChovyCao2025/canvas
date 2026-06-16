package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 承载 CdpWarehouseWatermark 表的持久化字段。
 */
@TableName("cdp_warehouse_watermark")
public class CdpWarehouseWatermarkDO {

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
     * job Name。
     */
    private String jobName;

    /**
     * watermark Time。
     */
    private LocalDateTime watermarkTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

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
     * 返回job Name。
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * 设置job Name。
     */
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    /**
     * 返回watermark Time。
     */
    public LocalDateTime getWatermarkTime() {
        return watermarkTime;
    }

    /**
     * 设置watermark Time。
     */
    public void setWatermarkTime(LocalDateTime watermarkTime) {
        this.watermarkTime = watermarkTime;
    }

    /**
     * 返回更新时间。
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
