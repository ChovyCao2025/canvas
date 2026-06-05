package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_realtime_audience_event_log")
public class CdpRealtimeAudienceEventLogDO {
    public static final String ADD = "ADD";
    public static final String REMOVE = "REMOVE";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long audienceId;
    private String sourceEventId;
    private String userId;
    private String operation;
    private LocalDateTime eventTime;
    private LocalDateTime processedAt;
}
