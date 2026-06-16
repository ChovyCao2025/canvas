package org.chovy.canvas.canvas.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasStatsFacade;
import org.chovy.canvas.canvas.domain.CanvasStatsCatalog;
import org.springframework.stereotype.Service;

/**
 * 封装CanvasStatsApplicationService相关的业务逻辑。
 */
@Service
public class CanvasStatsApplicationService implements CanvasStatsFacade {

    /**
     * 保存catalog。
     */
    private final CanvasStatsCatalog catalog;

    /**
     * 创建当前对象实例。
     */
    public CanvasStatsApplicationService() {
        this(Clock.systemDefaultZone());
    }

    /**
     * 创建当前对象实例。
     */
    CanvasStatsApplicationService(Clock clock) {
        this.catalog = new CanvasStatsCatalog(clock);
    }

    /**
     * 处理trace。
     */
    @Override
    public List<Map<String, Object>> trace(Long canvasId, String executionId) {
        return catalog.trace(canvasId, executionId);
    }

    /**
     * 处理recentExecutions。
     */
    @Override
    public List<Map<String, Object>> recentExecutions(Long canvasId, int size) {
        return catalog.recentExecutions(canvasId, size);
    }

    /**
     * 处理stats。
     */
    @Override
    public Map<String, Object> stats(Long canvasId, int days, String since, String until) {
        return catalog.stats(canvasId, days, since, until);
    }

    /**
     * 处理funnel。
     */
    @Override
    public List<Map<String, Object>> funnel(Long canvasId) {
        return catalog.funnel(canvasId);
    }

    /**
     * 处理trend。
     */
    @Override
    public List<Map<String, Object>> trend(Long canvasId, int days, String since, String until) {
        return catalog.trend(canvasId, days, since, until);
    }

    /**
     * 处理receipts。
     */
    @Override
    public Map<String, Object> receipts(Long canvasId) {
        return catalog.receipts(canvasId);
    }

    /**
     * 处理attributionSummary。
     */
    @Override
    public Map<String, Object> attributionSummary(Long canvasId) {
        return catalog.attributionSummary(canvasId);
    }
}
