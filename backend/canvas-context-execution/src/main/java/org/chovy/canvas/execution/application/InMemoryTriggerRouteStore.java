package org.chovy.canvas.execution.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.chovy.canvas.execution.application.CanvasTriggerApplicationService.TriggerRoute;

/**
 * 定义 InMemoryTriggerRouteStore 的执行上下文数据结构或业务契约。
 */
public class InMemoryTriggerRouteStore implements TriggerRouteStore {

    private final CopyOnWriteArrayList<TriggerRoute> routes = new CopyOnWriteArrayList<>();

    /**
     * 执行 save 对应的业务处理。
     * @param route route 参数
     */
    @Override
    public void save(TriggerRoute route) {
        remove(route.tenantId(), route.canvasId());
        routes.add(route);
    }

    /**
     * 执行 remove 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     */
    @Override
    public void remove(Long tenantId, Long canvasId) {
        routes.removeIf(route -> route.tenantId().equals(tenantId) && route.canvasId().equals(canvasId));
    }

    /**
     * 执行 routes 对应的业务处理。
     * @return 处理后的结果
     */
    @Override
    public List<TriggerRoute> routes() {
        return List.copyOf(routes);
    }

    /**
     * 执行 routesFor 对应的业务处理。
     * @param triggerType triggerType 参数
     * @param matchKey matchKey 参数
     * @return 处理后的结果
     */
    @Override
    public List<TriggerRoute> routesFor(String triggerType, String matchKey) {
        String normalizedType = triggerType == null || triggerType.isBlank()
                ? "MANUAL"
                : triggerType.toUpperCase(Locale.ROOT);
        List<TriggerRoute> result = new ArrayList<>();
        for (TriggerRoute route : routes) {
            if (!route.triggerType().equals(normalizedType)) {
                continue;
            }
            if (route.matchKey().isBlank() || route.matchKey().equals(matchKey == null ? "" : matchKey)) {
                result.add(route);
            }
        }
        return List.copyOf(result);
    }
}
