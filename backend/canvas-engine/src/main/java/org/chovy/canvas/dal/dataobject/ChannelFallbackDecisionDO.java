package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("channel_fallback_decision")
public class ChannelFallbackDecisionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String executionId;

    private String nodeId;

    private String originalChannel;

    private String originalProvider;

    private String finalChannel;

    private String finalProvider;

    private String decisionReason;

    private String attemptChainJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
