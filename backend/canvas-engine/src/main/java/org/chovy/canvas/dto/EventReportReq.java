package org.chovy.canvas.dto;

import lombok.Data;

import java.util.Map;

@Data
public class EventReportReq {
    /** 事件编码，必须在 event_definition 中已定义 */
    private String eventCode;
    /** 触发用户 ID */
    private String userId;
    /** 事件属性，key-value 结构，与事件定义中的 attributes 对应 */
    private Map<String, Object> attributes;
    /** 幂等 key，可选 */
    private String idempotencyKey;
}