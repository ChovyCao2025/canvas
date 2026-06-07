package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_sla_breach")
public class ConversationSlaBreachDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workItemId;

    private String breachType;

    private String severity;

    private String status;

    private String escalationTarget;

    private String reason;

    private LocalDateTime dueAt;

    private LocalDateTime breachedAt;

    private String resolvedBy;

    private LocalDateTime resolvedAt;

    private String metadataJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
