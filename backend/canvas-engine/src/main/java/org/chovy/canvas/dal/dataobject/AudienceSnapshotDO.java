package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audience_snapshot")
public class AudienceSnapshotDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long audienceId;
    private Long canvasId;
    private Long canvasVersionId;
    private String nodeId;
    private String snapshotMode;
    private Long userCount;
    private String userIdsJson;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
