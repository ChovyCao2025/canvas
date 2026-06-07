package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("growth_activity_event")
public class GrowthActivityEventDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long activityId;

    private Long participantId;

    private String eventType;

    private String eventKey;

    private String sourceType;

    private Long sourceId;

    private String payloadJson;

    private String createdBy;

    private LocalDateTime occurredAt;
}
