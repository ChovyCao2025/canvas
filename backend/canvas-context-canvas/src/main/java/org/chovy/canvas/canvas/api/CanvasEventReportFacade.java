package org.chovy.canvas.canvas.api;

import java.util.Map;

/**
 * 定义CanvasEventReportFacade对外提供的能力契约。
 */
public interface CanvasEventReportFacade {

    /**
     * 处理report。
     */
    Map<String, Object> report(String rawBody);
}
