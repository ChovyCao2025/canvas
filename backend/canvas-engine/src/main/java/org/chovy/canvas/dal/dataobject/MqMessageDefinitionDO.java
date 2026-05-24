package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * MQ 消息定义（mq_message_definition）。
 *
 * <p>运营在此注册可用于触发画布的 MQ 消息类型，
 * MQ_TRIGGER 节点通过 messageCode（或 topic）关联对应消息定义。
 * 执行引擎收到 MQ 消息后，根据 topic 路由到匹配的画布。
 */
@Data
@TableName("mq_message_definition")
public class MqMessageDefinitionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息显示名称 */
    private String name;

    /** 消息唯一标识，MQ_TRIGGER 节点通过此 code 选择消息类型 */
    private String messageCode;

    /** MQ Topic，消息投递时使用，执行引擎按此路由触发画布 */
    private String topic;

    /**
     * 消息体 Schema，JSON 数组，格式：
     * {@code [{"name":"orderId","displayName":"订单号","type":"STRING","required":true}]}
     * 执行时将消息体字段注入 ExecutionContext，供后续节点引用。
     */
    private String requestSchema;

    /** 消息描述 */
    private String description;

    /** 是否启用，1=启用，0=禁用 */
    private Integer enabled;

    /** 创建人 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
