package org.chovy.canvas.engine.disruptor;

import org.chovy.canvas.engine.lane.ExecutionLane;

import java.util.Map;

/**
 * Disruptor Ring Buffer 事件对象（设计文档 12.8节）。
 * 预分配，被 Ring Buffer 复用，execute() 前必须 reset()。
 */
public class CanvasExecutionEvent {

    /** 目标画布 ID。 */
    public Long   canvasId;

    /** 本次触发用户 ID。 */
    public String userId;

    /** 触发类型（EVENT/MQ/SCHEDULED 等）。 */
    public String triggerType;

    /** 触发节点类型（用于路由启动节点）。 */
    public String triggerNodeType;

    /** 触发匹配键（如 messageCode/eventCode）。 */
    public String matchKey;

    /** 触发载荷。 */
    public Map<String, Object> payload;

    /** 消息 ID（用于 MQ 幂等/追踪）。 */
    public String msgId;
    /** 预解析执行 lane。为空时由执行服务按触发上下文解析。 */
    public ExecutionLane executionLane;
    /** 请求模式下对应的执行请求 ID。 */
    public String requestId;
    /** Disruptor 内部派发选项，如溢出重试元数据。 */
    public CanvasDisruptorService.DispatchOptions dispatchOptions;

    /** 归还对象前清空字段，防止复用污染下一条事件。 */
    public void reset() {
        canvasId = null; userId = null; triggerType = null;
        triggerNodeType = null; matchKey = null; payload = null; msgId = null;
        executionLane = null; requestId = null; dispatchOptions = null;
    }
}
