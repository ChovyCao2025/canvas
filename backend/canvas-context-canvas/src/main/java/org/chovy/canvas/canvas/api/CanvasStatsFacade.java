package org.chovy.canvas.canvas.api;

import java.util.List;
import java.util.Map;

/**
 * 定义CanvasStatsFacade对外提供的能力契约。
 */
public interface CanvasStatsFacade {

    /**
     * 处理trace。
     */
    List<Map<String, Object>> trace(Long canvasId, String executionId);

    /**
     * 处理recentExecutions。
     */
    List<Map<String, Object>> recentExecutions(Long canvasId, int size);

    /**
     * 处理stats。
     */
    Map<String, Object> stats(Long canvasId, int days, String since, String until);

    /**
     * 处理funnel。
     */
    List<Map<String, Object>> funnel(Long canvasId);

    /**
     * 处理trend。
     */
    List<Map<String, Object>> trend(Long canvasId, int days, String since, String until);

    /**
     * 处理receipts。
     */
    Map<String, Object> receipts(Long canvasId);

    /**
     * 处理attributionSummary。
     */
    Map<String, Object> attributionSummary(Long canvasId);
}
