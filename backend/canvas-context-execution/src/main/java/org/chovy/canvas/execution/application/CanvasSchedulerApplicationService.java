package org.chovy.canvas.execution.application;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.execution.domain.DagGraph;
import org.springframework.stereotype.Service;

@Service
public class CanvasSchedulerApplicationService {

    private final CopyOnWriteArrayList<ScheduleRegistration> registrations = new CopyOnWriteArrayList<>();

    public void register(PublishedCanvasDefinition definition, DagGraph graph) {
        String cron = optionText(definition.executionOptions(), "scheduleCron");
        registrations.removeIf(registration -> registration.tenantId().equals(definition.tenantId())
                && registration.canvasId().equals(definition.canvasId()));
        if (!cron.isBlank()) {
            registrations.add(new ScheduleRegistration(
                    definition.tenantId(),
                    definition.canvasId(),
                    definition.versionId(),
                    cron));
        }
    }

    public void unregister(Long tenantId, Long canvasId) {
        registrations.removeIf(registration -> registration.tenantId().equals(tenantId)
                && registration.canvasId().equals(canvasId));
    }

    public List<ScheduleRegistration> registrations() {
        return List.copyOf(registrations);
    }

    private static String optionText(Map<String, Object> options, String key) {
        Object value = options == null ? null : options.get(key);
        return value == null ? "" : value.toString();
    }

    public record ScheduleRegistration(
            Long tenantId,
            Long canvasId,
            Long versionId,
            String cron) {
    }
}
