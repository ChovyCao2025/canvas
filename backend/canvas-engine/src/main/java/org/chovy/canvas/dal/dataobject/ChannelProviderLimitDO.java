package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ChannelProviderLimitDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("channel_provider_limit")
public class ChannelProviderLimitDO {

    /** 渠道服务商限制主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 渠道服务商限制触达渠道 */
    private String channel;

    /** 渠道服务商限制服务商 */
    private String provider;

    /** 渠道服务商限制操作 */
    private String operation;

    /** 渠道服务商限制每秒限制 */
    private Integer perSecondLimit;

    /** 渠道服务商限制每日限制 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long dailyLimit;

    /** 渠道服务商限制失败闭环 */
    private Integer failClosed;

    /** 渠道服务商限制最后更新人 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String updatedBy;

    /** 渠道服务商限制最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
