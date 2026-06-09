package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ChannelFallbackPolicyDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("channel_fallback_policy")
public class ChannelFallbackPolicyDO {

    /** 渠道兜底策略主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 渠道兜底策略触达渠道 */
    private String channel;

    /** 渠道兜底策略服务商 */
    private String provider;

    /** 渠道兜底策略兜底渠道 */
    private String fallbackChannel;

    /** 渠道兜底策略兜底服务商 */
    private String fallbackProvider;

    /** 渠道兜底策略是否启用 */
    private Integer enabled;

    /** 渠道兜底策略原因说明 */
    private String reason;

    /** 渠道兜底策略最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
