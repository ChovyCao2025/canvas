package org.chovy.canvas.execution.application;

import java.util.Objects;

import org.chovy.canvas.canvas.api.ExecutionPublicationPort;
import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.execution.domain.DagGraph;
import org.chovy.canvas.execution.domain.DagRuntimeService;
import org.springframework.stereotype.Service;

@Service
public class ExecutionPublicationApplicationService implements ExecutionPublicationPort {

    private final DagRuntimeService dagRuntimeService;
    private final CanvasTriggerApplicationService triggerService;
    private final CanvasSchedulerApplicationService schedulerService;
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

    @Override
    public void publish(PublishedCanvasDefinition definition) {
        Objects.requireNonNull(definition, "definition is required");
        DagGraph graph = dagRuntimeService.validate(definition);
        triggerService.register(definition, graph);
        schedulerService.register(definition, graph);
        definitionRepository.save(definition);
    }

    @Override
    public void unpublish(Long tenantId, Long canvasId) {
        triggerService.unregister(tenantId, canvasId);
        schedulerService.unregister(tenantId, canvasId);
        definitionRepository.remove(tenantId, canvasId);
    }
}
