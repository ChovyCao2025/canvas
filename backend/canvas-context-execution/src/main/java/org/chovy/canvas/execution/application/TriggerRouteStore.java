package org.chovy.canvas.execution.application;

import java.util.List;

import org.chovy.canvas.execution.application.CanvasTriggerApplicationService.TriggerRoute;

public interface TriggerRouteStore {

    void save(TriggerRoute route);

    void remove(Long tenantId, Long canvasId);

    List<TriggerRoute> routes();

    List<TriggerRoute> routesFor(String triggerType, String matchKey);
}
