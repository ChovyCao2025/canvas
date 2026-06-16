package org.chovy.canvas.bi.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
/**
 * BiDatasourceHealthSnapshotDO 持久化对象。
 */
@TableName("bi_datasource_health_snapshot")
public class BiDatasourceHealthSnapshotDO {
    /**
     * 唯一标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * sourceKey 对应的业务键。
     */
    private String sourceKey;

    /**
     * sourceType 字段值。
     */
    private String sourceType;

    /**
     * available 字段值。
     */
    private Boolean available;

    /**
     * message 字段值。
     */
    private String message;

    /**
     * checkedAt 对应的时间。
     */
    private LocalDateTime checkedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

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
     * 获取 Source Key。
     */
    public String getSourceKey() {
        return sourceKey;
    }
    /**
     * 设置 Source Key。
     */
    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }
    /**
     * 获取 Source Type。
     */
    public String getSourceType() {
        return sourceType;
    }
    /**
     * 设置 Source Type。
     */
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    /**
     * 获取 Available。
     */
    public Boolean getAvailable() {
        return available;
    }
    /**
     * 设置 Available。
     */
    public void setAvailable(Boolean available) {
        this.available = available;
    }
    /**
     * 获取 Message。
     */
    public String getMessage() {
        return message;
    }
    /**
     * 设置 Message。
     */
    public void setMessage(String message) {
        this.message = message;
    }
    /**
     * 获取 Checked At。
     */
    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }
    /**
     * 设置 Checked At。
     */
    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
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
}
