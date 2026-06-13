package org.chovy.canvas.execution.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.chovy.canvas.execution.application.CanvasTriggerApplicationService.TriggerRoute;

public class InMemoryTriggerRouteStore implements TriggerRouteStore {

    private final CopyOnWriteArrayList<TriggerRoute> routes = new CopyOnWriteArrayList<>();

    @Override
    public void save(TriggerRoute route) {
        remove(route.tenantId(), route.canvasId());
        routes.add(route);
    }

    @Override
    public void remove(Long tenantId, Long canvasId) {
        routes.removeIf(route -> route.tenantId().equals(tenantId) && route.canvasId().equals(canvasId));
    }

    @Override
    public List<TriggerRoute> routes() {
        return List.copyOf(routes);
    }

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
