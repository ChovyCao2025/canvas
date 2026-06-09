package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AudienceSnapshotDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("audience_snapshot")
public class AudienceSnapshotDO {
    /** 人群快照主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的人群 ID */
    private Long audienceId;
    /** 关联的画布 ID */
    private Long canvasId;
    /** 关联的画布版本 ID */
    private Long canvasVersionId;
    /** 关联的节点 ID */
    private String nodeId;
    /** 人群快照快照模式 */
    private String snapshotMode;
    /** 人群快照用户数量 */
    private Long userCount;
    /** 人群快照用户 ID 列表 JSON */
    private String userIdsJson;
    /** 人群快照创建人 */
    private String createdBy;
    /** 人群快照创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
