package org.chovy.canvas.canvas.api;

import java.util.List;

/**
 * 定义HomeOverviewFacade对外提供的能力契约。
 */
public interface HomeOverviewFacade {

    /**
     * 处理overview。
     */
    HomeOverviewView overview(int days);

    /**
     * 承载HomeOverviewView的数据快照。
     */
    record HomeOverviewView(
            /**
             * 记录range。
             */
            RangeView range,
            /**
             * 记录summary。
             */
            SummaryView summary,
            /**
             * 记录trend。
             */
            List<TrendPointView> trend,
            /**
             * 记录topCanvases。
             */
            List<TopCanvasView> topCanvases,
            /**
             * 记录attentionItems。
             */
            List<AttentionItemView> attentionItems) {
    }

    /**
     * 承载RangeView的数据快照。
     */
    record RangeView(int days, String since, String until) {
    }

    /**
     * 承载SummaryView的数据快照。
     */
    record SummaryView(
            /**
             * 记录publishedCanvasCount。
             */
            long publishedCanvasCount,
            /**
             * 记录totalExecutions。
             */
            long totalExecutions,
            /**
             * 记录uniqueUsers。
             */
            long uniqueUsers,
            /**
             * 记录failedExecutions。
             */
            long failedExecutions,
            /**
             * 记录successRate。
             */
            String successRate) {
    }

    /**
     * 承载TrendPointView的数据快照。
     */
    record TrendPointView(String date, long total, long failed) {
    }

    /**
     * 承载TopCanvasView的数据快照。
     */
    record TopCanvasView(
            /**
             * 记录画布标识。
             */
            Long canvasId,
            /**
             * 记录名称。
             */
            String name,
            /**
             * 记录总数。
             */
            long total,
            /**
             * 记录uniqueUsers。
             */
            long uniqueUsers,
            /**
             * 记录successRate。
             */
            String successRate,
            /**
             * 记录failed。
             */
            long failed) {
    }

    /**
     * 承载AttentionItemView的数据快照。
     */
    record AttentionItemView(
            /**
             * 记录画布标识。
             */
            Long canvasId,
            /**
             * 记录名称。
             */
            String name,
            /**
             * 记录类型。
             */
            String type,
            /**
             * 记录消息。
             */
            String message,
            /**
             * 记录severity。
             */
            String severity) {
    }
}
