package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_routing_rule")
public class ConversationRoutingRuleDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String ruleKey;

    private String channel;

    private String minPriority;

    private String requiredSkillsJson;

    private String targetTeam;

    private Integer slaMinutes;

    private Integer enabled;

    private Integer sortOrder;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
