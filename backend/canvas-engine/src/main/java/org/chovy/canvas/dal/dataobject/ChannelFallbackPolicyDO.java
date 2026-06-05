package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("channel_fallback_policy")
public class ChannelFallbackPolicyDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String channel;

    private String provider;

    private String fallbackChannel;

    private String fallbackProvider;

    private Integer enabled;

    private String reason;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
