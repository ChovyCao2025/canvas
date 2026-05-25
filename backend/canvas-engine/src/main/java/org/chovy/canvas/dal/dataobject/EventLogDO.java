package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 事件上报日志（event_log）。
 *
 * <p>每次业务方调用 {@code POST /canvas/events/report} 时写入一条记录，
 * 用于事件追踪和触发量统计。写入时间点：triggerAllCanvas 之后、resumeWaits 之前，
 * 因此 canvasTriggered 反映新触发的画布数量（不含本次 WAIT 恢复数量）。
 */
@Data
@TableName("event_log")
public class EventLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上报的事件标识，对应 EventDefinitionDO.eventCode */
    private String eventCode;

    /** 触发用户 ID */
    private String userId;

    /** 压测批次 ID，普通业务流量为空 */
    private String perfRunId;

    /**
     * 上报携带的业务属性，JSON 对象字符串。
     * 内容与 EventDefinitionDO.attributes 中定义的字段对应，
     * 执行引擎会将其注入 ExecutionContext，供后续节点通过 ${key} 引用。
     */
    private String attributes;

    /**
     * 本次事件新触发的画布数量（写入 Disruptor 的画布数，不含 WAIT 恢复）。
     * 0 表示无匹配画布（路由表为空或所有画布触发均失败）。
     */
    private Integer canvasTriggered;

    /**
     * 触发的画布总数（当前与 canvasTriggered 相同，预留字段以区分未来的触发方式）。
     */
    private Integer canvasCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
