package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * GrowthActivityParticipantDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("growth_activity_participant")
public class GrowthActivityParticipantDO {

    /** 增长活动参与人主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的活动 ID */
    private Long activityId;

    /** 关联的用户 ID */
    private String userId;

    /** 增长活动参与人当前状态 */
    private String status;

    /** 增长活动参与人加入时间 */
    private LocalDateTime joinedAt;

    /** 增长活动参与人事件属性 JSON */
    private String attributesJson;
}
