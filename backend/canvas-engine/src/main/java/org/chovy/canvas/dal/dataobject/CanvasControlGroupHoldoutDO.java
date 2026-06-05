package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("canvas_control_group_holdout")
public class CanvasControlGroupHoldoutDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long canvasId;
    private String userId;
    private String eventId;
    private String reason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
