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
@TableName("channel_provider_limit")
public class ChannelProviderLimitDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String channel;

    private String provider;

    private String operation;

    private Integer perSecondLimit;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long dailyLimit;

    private Integer failClosed;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
