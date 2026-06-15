package org.chovy.canvas.canvas.api;

import java.util.List;
import java.util.Map;

public interface CanvasStatsFacade {

    List<Map<String, Object>> trace(Long canvasId, String executionId);

    List<Map<String, Object>> recentExecutions(Long canvasId, int size);

    Map<String, Object> stats(Long canvasId, int days, String since, String until);

    List<Map<String, Object>> funnel(Long canvasId);

    List<Map<String, Object>> trend(Long canvasId, int days, String since, String until);

    Map<String, Object> receipts(Long canvasId);

    Map<String, Object> attributionSummary(Long canvasId);
}
