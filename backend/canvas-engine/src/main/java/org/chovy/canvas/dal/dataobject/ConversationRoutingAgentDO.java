package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_routing_agent")
public class ConversationRoutingAgentDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String agentKey;

    private String displayName;

    private String teamKey;

    private String status;

    private Integer maxCapacity;

    private Integer currentLoad;

    private String skillsJson;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
