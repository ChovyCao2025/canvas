package org.chovy.canvas.canvas.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 封装CanvasProjectDO相关的业务逻辑。
 */
@TableName("canvas_project")
public class CanvasProjectDO {

    /**
     * 保存标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 保存租户标识。
     */
    private Long tenantId;

    /**
     * 保存projectKey。
     */
    private String projectKey;

    /**
     * 保存projectName。
     */
    private String projectName;

    /**
     * 保存描述。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    /**
     * 保存状态。
     */
    private String status;

    /**
     * 保存default settingsJSON 内容。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String defaultSettingsJson;

    /**
     * 保存requireReviewBeforePublish。
     */
    private Integer requireReviewBeforePublish;

    /**
     * 保存quiet hoursJSON 内容。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String quietHoursJson;

    /**
     * 保存创建人。
     */
    private String createdBy;

    /**
     * 保存更新人。
     */
    private String updatedBy;

    /**
     * 保存创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 保存更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
