package org.chovy.canvas.execution.application;

import java.util.List;

import org.chovy.canvas.execution.application.CanvasTriggerApplicationService.TriggerRoute;

/**
 * 定义 TriggerRouteStore 的执行上下文数据结构或业务契约。
 */
public interface TriggerRouteStore {

    /**
     * 执行 save 对应的业务处理。
     * @param route route 参数
     */
    void save(TriggerRoute route);

    /**
     * 执行 remove 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     */
    void remove(Long tenantId, Long canvasId);

    /**
     * 执行 routes 对应的业务处理。
     * @return 处理后的结果
     */
    List<TriggerRoute> routes();

    /**
     * 执行 routesFor 对应的业务处理。
     * @param triggerType triggerType 参数
     * @param matchKey matchKey 参数
     * @return 处理后的结果
     */
    List<TriggerRoute> routesFor(String triggerType, String matchKey);
}
