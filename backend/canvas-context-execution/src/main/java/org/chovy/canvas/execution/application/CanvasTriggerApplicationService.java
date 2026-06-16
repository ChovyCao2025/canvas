package org.chovy.canvas.execution.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.execution.domain.DagGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 定义 CanvasTriggerApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class CanvasTriggerApplicationService {

    /**
     * 保存 routeStore 对应的状态或配置。
     */
    private final TriggerRouteStore routeStore;

    /**
     * 执行 CanvasTriggerApplicationService 对应的业务处理。
     */
    public CanvasTriggerApplicationService() {
        this(new InMemoryTriggerRouteStore());
    }

    /**
     * 执行 CanvasTriggerApplicationService 对应的业务处理。
     * @param routeStore routeStore 参数
     */
    @Autowired
    public CanvasTriggerApplicationService(TriggerRouteStore routeStore) {
        this.routeStore = routeStore;
    }

    /**
     * 执行 register 对应的业务处理。
     * @param definition definition 参数
     * @param graph graph 参数
     */
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

    /**
     * 执行 unregister 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     */
    public void unregister(Long tenantId, Long canvasId) {
        routeStore.remove(tenantId, canvasId);
    }

    /**
     * 执行 routes 对应的业务处理。
     * @return 处理后的结果
     */
    public List<TriggerRoute> routes() {
        return routeStore.routes();
    }

    /**
     * 执行 routesFor 对应的业务处理。
     * @param triggerType triggerType 参数
     * @param matchKey matchKey 参数
     * @return 处理后的结果
     */
    public List<TriggerRoute> routesFor(String triggerType, String matchKey) {
        return routeStore.routesFor(triggerType, matchKey);
    }

    /**
     * 执行 optionText 对应的业务处理。
     * @param options options 参数
     * @param key key 参数
     * @param fallback fallback 参数
     */
    private static String optionText(Map<String, Object> options, String key, String fallback) {
        Object value = options == null ? null : options.get(key);
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString();
    }

    /**
     * 定义 TriggerRoute 的执行上下文数据结构或业务契约。
     * @param tenantId tenantId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param versionId versionId 对应的数据字段
     * @param triggerType triggerType 对应的数据字段
     * @param matchKey matchKey 对应的数据字段
     */
    public record TriggerRoute(
            Long tenantId,
            Long canvasId,
            Long versionId,
            String triggerType,
            String matchKey) {
    }
}
