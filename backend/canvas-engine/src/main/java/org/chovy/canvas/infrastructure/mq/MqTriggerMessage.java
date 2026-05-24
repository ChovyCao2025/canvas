package org.chovy.canvas.infrastructure.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MQ 触发消息体。
 * 由 SendMqHandler 发送，MqTriggerConsumer 接收并路由到画布执行。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqTriggerMessage {
    /** 触发用户 ID */
    private String userId;
    /** 消息类型标识，对应 MqMessageDefinitionDO.messageCode */
    private String messageCode;
    /** 业务载荷，供画布节点通过上下文引用 */
    private Map<String, Object> payload;
}
