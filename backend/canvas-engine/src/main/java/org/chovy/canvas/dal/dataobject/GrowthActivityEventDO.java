package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * GrowthActivityEventDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("growth_activity_event")
public class GrowthActivityEventDO {

    /** 增长活动事件主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的活动 ID */
    private Long activityId;

    /** 关联的参与人 ID */
    private Long participantId;

    /** 增长活动事件事件类型 */
    private String eventType;

    /** 增长活动事件事件业务键 */
    private String eventKey;

    /** 增长活动事件来源类型 */
    private String sourceType;

    /** 增长活动事件来源 ID */
    private Long sourceId;

    /** 增长活动事件载荷 JSON */
    private String payloadJson;

    /** 增长活动事件创建人 */
    private String createdBy;

    /** 增长活动事件发生时间 */
    private LocalDateTime occurredAt;
}
