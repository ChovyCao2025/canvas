package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 承载 TagDefinition 表的持久化字段。
 */
@TableName("tag_definition")
public class TagDefinitionDO {

    /**
     * 唯一标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 名称。
     */
    private String name;

    /**
     * 标签编码。
     */
    private String tagCode;

    /**
     * tag Type。
     */
    private String tagType;

    /**
     * 描述。
     */
    private String description;

    /**
     * 启用标记。
     */
    private Integer enabled;

    /**
     * 值类型。
     */
    private String valueType;

    /**
     * manual Enabled。
     */
    private Integer manualEnabled;

    /**
     * default Ttl Days。
     */
    private Integer defaultTtlDays;

    /**
     * category。
     */
    private String category;

    /**
     * 负责人。
     */
    private String owner;

    /**
     * write Policy。
     */
    private String writePolicy;

    /**
     * 创建人。
     */
    private String createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

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
     * 返回名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 设置名称。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 返回标签编码。
     */
    public String getTagCode() {
        return tagCode;
    }

    /**
     * 设置标签编码。
     */
    public void setTagCode(String tagCode) {
        this.tagCode = tagCode;
    }

    /**
     * 返回tag Type。
     */
    public String getTagType() {
        return tagType;
    }

    /**
     * 设置tag Type。
     */
    public void setTagType(String tagType) {
        this.tagType = tagType;
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
     * 返回启用标记。
     */
    public Integer getEnabled() {
        return enabled;
    }

    /**
     * 设置启用标记。
     */
    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回值类型。
     */
    public String getValueType() {
        return valueType;
    }

    /**
     * 设置值类型。
     */
    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    /**
     * 返回manual Enabled。
     */
    public Integer getManualEnabled() {
        return manualEnabled;
    }

    /**
     * 设置manual Enabled。
     */
    public void setManualEnabled(Integer manualEnabled) {
        this.manualEnabled = manualEnabled;
    }

    /**
     * 返回default Ttl Days。
     */
    public Integer getDefaultTtlDays() {
        return defaultTtlDays;
    }

    /**
     * 设置default Ttl Days。
     */
    public void setDefaultTtlDays(Integer defaultTtlDays) {
        this.defaultTtlDays = defaultTtlDays;
    }

    /**
     * 返回category。
     */
    public String getCategory() {
        return category;
    }

    /**
     * 设置category。
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 返回负责人。
     */
    public String getOwner() {
        return owner;
    }

    /**
     * 设置负责人。
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * 返回write Policy。
     */
    public String getWritePolicy() {
        return writePolicy;
    }

    /**
     * 设置write Policy。
     */
    public void setWritePolicy(String writePolicy) {
        this.writePolicy = writePolicy;
    }

    /**
     * 返回创建人。
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

    /**
     * 返回创建时间。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
