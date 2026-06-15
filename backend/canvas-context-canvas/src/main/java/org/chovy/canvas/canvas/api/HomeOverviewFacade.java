package org.chovy.canvas.canvas.api;

import java.util.List;

public interface HomeOverviewFacade {

    HomeOverviewView overview(int days);

    record HomeOverviewView(
            RangeView range,
            SummaryView summary,
            List<TrendPointView> trend,
            List<TopCanvasView> topCanvases,
            List<AttentionItemView> attentionItems) {
    }

    record RangeView(int days, String since, String until) {
    }

    record SummaryView(
            long publishedCanvasCount,
            long totalExecutions,
            long uniqueUsers,
            long failedExecutions,
            String successRate) {
    }

    record TrendPointView(String date, long total, long failed) {
    }

    record TopCanvasView(
            Long canvasId,
            String name,
            long total,
            long uniqueUsers,
            String successRate,
            long failed) {
    }

    record AttentionItemView(
            Long canvasId,
            String name,
            String type,
            String message,
            String severity) {
    }
}
