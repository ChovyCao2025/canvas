package org.chovy.canvas.execution.application;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.execution.domain.DagGraph;
import org.springframework.stereotype.Service;

/**
 * 定义 CanvasSchedulerApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class CanvasSchedulerApplicationService {

    private final CopyOnWriteArrayList<ScheduleRegistration> registrations = new CopyOnWriteArrayList<>();

    /**
     * 执行 register 对应的业务处理。
     * @param definition definition 参数
     * @param graph graph 参数
     */
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

    /**
     * 执行 unregister 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     */
    public void unregister(Long tenantId, Long canvasId) {
        registrations.removeIf(registration -> registration.tenantId().equals(tenantId)
                && registration.canvasId().equals(canvasId));
    }

    /**
     * 执行 registrations 对应的业务处理。
     * @return 处理后的结果
     */
    public List<ScheduleRegistration> registrations() {
        return List.copyOf(registrations);
    }

    /**
     * 执行 optionText 对应的业务处理。
     * @param options options 参数
     * @param key key 参数
     */
    private static String optionText(Map<String, Object> options, String key) {
        Object value = options == null ? null : options.get(key);
        return value == null ? "" : value.toString();
    }

    /**
     * 定义 ScheduleRegistration 的执行上下文数据结构或业务契约。
     * @param tenantId tenantId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param versionId versionId 对应的数据字段
     * @param cron cron 对应的数据字段
     */
    public record ScheduleRegistration(
            Long tenantId,
            Long canvasId,
            Long versionId,
            String cron) {
    }
}
