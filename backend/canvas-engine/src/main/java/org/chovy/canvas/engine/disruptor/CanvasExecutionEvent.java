package org.chovy.canvas.engine.disruptor;

import java.util.Map;

/**
 * Disruptor Ring Buffer 事件对象（设计文档 12.8节）。
 * 预分配，被 Ring Buffer 复用，execute() 前必须 reset()。
 */
public class CanvasExecutionEvent {

    public Long   canvasId;
    public String userId;
    public String triggerType;
    public String triggerNodeType;
    public String matchKey;
    public Map<String, Object> payload;
    public String msgId;
    public CanvasDisruptorService.DispatchOptions dispatchOptions;

    public void reset() {
        canvasId = null; userId = null; triggerType = null;
        triggerNodeType = null; matchKey = null; payload = null; msgId = null;
        dispatchOptions = null;
    }
}
