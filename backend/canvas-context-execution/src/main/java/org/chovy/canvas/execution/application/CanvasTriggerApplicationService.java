package org.chovy.canvas.execution.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.execution.domain.DagGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("executionCanvasTriggerApplicationService")
public class CanvasTriggerApplicationService {

    private final TriggerRouteStore routeStore;

    public CanvasTriggerApplicationService() {
        this(new InMemoryTriggerRouteStore());
    }

    @Autowired
    public CanvasTriggerApplicationService(TriggerRouteStore routeStore) {
        this.routeStore = routeStore;
    }

    public void register(PublishedCanvasDefinition definition, DagGraph graph) {
        String triggerType = optionText(definition.executionOptions(), "triggerType", "MANUAL");
        String matchKey = optionText(definition.executionOptions(), "topicKey", "");
        if (matchKey.isBlank()) {
            matchKey = optionText(definition.executionOptions(), "eventCode", "");
        }
        routeStore.remove(definition.tenantId(), definition.canvasId());
        routeStore.save(new TriggerRoute(
                definition.tenantId(),
                definition.canvasId(),
                definition.versionId(),
                triggerType.toUpperCase(Locale.ROOT),
                matchKey));
    }

    public void unregister(Long tenantId, Long canvasId) {
        routeStore.remove(tenantId, canvasId);
    }

    public List<TriggerRoute> routes() {
        return routeStore.routes();
    }

    public List<TriggerRoute> routesFor(String triggerType, String matchKey) {
        return routeStore.routesFor(triggerType, matchKey);
    }

    private static String optionText(Map<String, Object> options, String key, String fallback) {
        Object value = options == null ? null : options.get(key);
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString();
    }

    public record TriggerRoute(
            Long tenantId,
            Long canvasId,
            Long versionId,
            String triggerType,
            String matchKey) {
    }
}
