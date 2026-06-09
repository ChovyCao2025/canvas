package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CanvasControlGroupHoldoutDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("canvas_control_group_holdout")
public class CanvasControlGroupHoldoutDO {
    /** 画布控制分组对照组主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的画布 ID */
    private Long canvasId;
    /** 关联的用户 ID */
    private String userId;
    /** 关联的事件 ID */
    private String eventId;
    /** 画布控制分组对照组原因说明 */
    private String reason;
    /** 画布控制分组对照组创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
