package org.chovy.canvas.execution.application;

import java.util.Objects;

import org.chovy.canvas.canvas.api.ExecutionPublicationPort;
import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.execution.domain.DagGraph;
import org.chovy.canvas.execution.domain.DagRuntimeService;
import org.springframework.stereotype.Service;

/**
 * 定义 ExecutionPublicationApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class ExecutionPublicationApplicationService implements ExecutionPublicationPort {

    /**
     * 保存 dagRuntimeService 对应的状态或配置。
     */
    private final DagRuntimeService dagRuntimeService;

    /**
     * 保存 triggerService 对应的状态或配置。
     */
    private final CanvasTriggerApplicationService triggerService;

    /**
     * 保存 schedulerService 对应的状态或配置。
     */
    private final CanvasSchedulerApplicationService schedulerService;

    /**
     * 保存 definitionRepository 对应的状态或配置。
     */
    private final ExecutionDefinitionRepository definitionRepository;

    public ExecutionPublicationApplicationService(
            DagRuntimeService dagRuntimeService,
            CanvasTriggerApplicationService triggerService,
            CanvasSchedulerApplicationService schedulerService,
            ExecutionDefinitionRepository definitionRepository) {
        this.dagRuntimeService = Objects.requireNonNull(dagRuntimeService, "dagRuntimeService is required");
        this.triggerService = Objects.requireNonNull(triggerService, "triggerService is required");
        this.schedulerService = Objects.requireNonNull(schedulerService, "schedulerService is required");
        this.definitionRepository = Objects.requireNonNull(definitionRepository, "definitionRepository is required");
    }

    /**
     * 执行 publish 对应的业务处理。
     * @param definition definition 参数
     */
    @Override
    public void publish(PublishedCanvasDefinition definition) {
        Objects.requireNonNull(definition, "definition is required");
        DagGraph graph = dagRuntimeService.validate(definition);
        triggerService.register(definition, graph);
        schedulerService.register(definition, graph);
        definitionRepository.save(definition);
    }

    /**
     * 执行 unpublish 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     */
    @Override
    public void unpublish(Long tenantId, Long canvasId) {
        triggerService.unregister(tenantId, canvasId);
        schedulerService.unregister(tenantId, canvasId);
        definitionRepository.remove(tenantId, canvasId);
    }
}
