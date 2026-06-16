package org.chovy.canvas.canvas.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 封装CanvasVersionDO相关的业务逻辑。
 */
@TableName("canvas_version")
public class CanvasVersionDO {

    /**
     * 保存标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 保存租户标识。
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 保存画布标识。
     */
    private Long canvasId;

    /**
     * 保存version。
     */
    private Integer version;

    /**
     * 保存graphJSON 内容。
     */
    private String graphJson;

    /**
     * 保存状态。
     */
    private Integer status;

    /**
     * 保存创建人。
     */
    private String createdBy;

    /**
     * 保存创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 获取标识。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置标识。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取租户标识。
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
     * 获取画布标识。
     */
    public Long getCanvasId() {
        return canvasId;
    }

    /**
     * 设置画布标识。
     */
    public void setCanvasId(Long canvasId) {
        this.canvasId = canvasId;
    }

    /**
     * 获取Version。
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * 设置Version。
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * 获取graphJSON 内容。
     */
    public String getGraphJson() {
        return graphJson;
    }

    /**
     * 设置graphJSON 内容。
     */
    public void setGraphJson(String graphJson) {
        this.graphJson = graphJson;
    }

    /**
     * 获取状态。
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置状态。
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 获取创建人。
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 设置创建人。
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
