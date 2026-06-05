package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("analytics_event")
public class AnalyticsEventDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String eventCode;

    private String userId;

    private String anonymousId;

    private String sessionId;

    private String platform;

    private String deviceType;

    private String source;

    private LocalDateTime eventTime;

    private LocalDateTime receivedAt;

    private Integer schemaVersion;

    private BigDecimal businessValue;

    private String attributesJson;

    private String retentionClass;

    private String archiveStatus;

    private LocalDateTime archivedAt;

    private Boolean legalHold;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
