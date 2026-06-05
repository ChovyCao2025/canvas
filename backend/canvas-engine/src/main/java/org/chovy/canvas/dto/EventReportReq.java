package org.chovy.canvas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 外部事件上报请求。
 *
 * <p>由业务系统调用事件上报 API 触发画布执行。
 * 典型路径：`eventCode` 定位触发路由 -> 组装 payload -> 执行引擎入队。
 */
@Data
public class EventReportReq {

    /** 事件编码，必须在 `event_definition` 中已定义。 */
    @NotBlank
    @Size(max = 128)
    private String eventCode;

    /** 触发用户 ID（用于人群/配额/上下文聚合主键）。 */
    @NotBlank
    @Size(max = 128)
    private String userId;

    /** 事件属性，key-value 结构，与事件定义中的 attributes 对应。 */
    private Map<String, Object> attributes;

    /** 幂等 key，可选（用于上报重试去重）。 */
    @Size(max = 128)
    private String idempotencyKey;
}
