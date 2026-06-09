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
 * ChannelConnectorDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("channel_connector")
public class ChannelConnectorDO {

    /** 渠道连接器主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 渠道连接器连接器业务键 */
    private String connectorKey;

    /** 渠道连接器触达渠道 */
    private String channel;

    /** 渠道连接器服务商 */
    private String provider;

    /** 渠道连接器模式 */
    private String mode;

    /** 渠道连接器能力明细 JSON */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String capabilitiesJson;

    /** 渠道连接器健康状态 */
    private String healthStatus;

    /** 渠道连接器健康消息 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String healthMessage;

    /** CHANNELCONNECTORDISABLEDREASON */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String disabledReason;

    /** 渠道连接器最近检查时间 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime lastCheckedAt;

    /** 渠道连接器创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 渠道连接器最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
