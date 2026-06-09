package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpRealtimeAudienceEventLogDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_realtime_audience_event_log")
public class CdpRealtimeAudienceEventLogDO {
    public static final String ADD = "ADD";
    public static final String REMOVE = "REMOVE";

    /** CDP实时人群事件日志主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的人群 ID */
    private Long audienceId;
    /** 关联的来源事件 ID */
    private String sourceEventId;
    /** 关联的用户 ID */
    private String userId;
    /** CDP实时人群事件日志操作 */
    private String operation;
    /** CDP实时人群事件日志事件发生时间 */
    private LocalDateTime eventTime;
    /** CDP实时人群事件日志处理时间 */
    private LocalDateTime processedAt;
}
