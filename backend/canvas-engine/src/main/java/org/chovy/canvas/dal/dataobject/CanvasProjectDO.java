package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CanvasProjectDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("canvas_project")
public class CanvasProjectDO {
    /** 画布项目主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 画布项目项目业务键 */
    private String projectKey;
    /** 画布项目项目名称 */
    private String projectName;
    /** 画布项目说明 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    /** 画布项目当前状态 */
    private String status;
    /** 画布项目默认设置 JSON */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String defaultSettingsJson;
    /** 画布项目要求评审之前发布 */
    private Integer requireReviewBeforePublish;
    /** 画布项目免打扰时段 JSON */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String quietHoursJson;
    /** 画布项目创建人 */
    private String createdBy;
    /** 画布项目最后更新人 */
    private String updatedBy;
    /** 画布项目创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 画布项目最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
