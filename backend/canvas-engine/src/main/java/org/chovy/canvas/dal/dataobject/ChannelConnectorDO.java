package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("channel_connector")
public class ChannelConnectorDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String connectorKey;

    private String channel;

    private String provider;

    private String mode;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String capabilitiesJson;

    private String healthStatus;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String healthMessage;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String disabledReason;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime lastCheckedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
