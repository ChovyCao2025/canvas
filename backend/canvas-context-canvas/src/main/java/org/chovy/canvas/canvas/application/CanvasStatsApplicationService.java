package org.chovy.canvas.canvas.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasStatsFacade;
import org.chovy.canvas.canvas.domain.CanvasStatsCatalog;
import org.springframework.stereotype.Service;

@Service
public class CanvasStatsApplicationService implements CanvasStatsFacade {

    private final CanvasStatsCatalog catalog;

    public CanvasStatsApplicationService() {
        this(Clock.systemDefaultZone());
    }

    CanvasStatsApplicationService(Clock clock) {
        this.catalog = new CanvasStatsCatalog(clock);
    }

    @Override
    public List<Map<String, Object>> trace(Long canvasId, String executionId) {
        return catalog.trace(canvasId, executionId);
    }

    @Override
    public List<Map<String, Object>> recentExecutions(Long canvasId, int size) {
        return catalog.recentExecutions(canvasId, size);
    }

    @Override
    public Map<String, Object> stats(Long canvasId, int days, String since, String until) {
        return catalog.stats(canvasId, days, since, until);
    }

    @Override
    public List<Map<String, Object>> funnel(Long canvasId) {
        return catalog.funnel(canvasId);
    }

    @Override
    public List<Map<String, Object>> trend(Long canvasId, int days, String since, String until) {
        return catalog.trend(canvasId, days, since, until);
    }

    @Override
    public Map<String, Object> receipts(Long canvasId) {
        return catalog.receipts(canvasId);
    }

    @Override
    public Map<String, Object> attributionSummary(Long canvasId) {
        return catalog.attributionSummary(canvasId);
    }
}
