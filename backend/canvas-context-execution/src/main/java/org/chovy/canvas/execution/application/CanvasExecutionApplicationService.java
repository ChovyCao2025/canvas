package org.chovy.canvas.execution.application;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.execution.api.CanvasExecutionFacade;
import org.chovy.canvas.execution.api.trace.ExecutionTraceView;
import org.chovy.canvas.execution.domain.DagGraph;
import org.chovy.canvas.execution.domain.DagNode;
import org.chovy.canvas.execution.domain.DagRuntimeService;
import org.chovy.canvas.execution.domain.NodeExecutionContext;
import org.chovy.canvas.execution.domain.NodeExecutionResult;
import org.chovy.canvas.execution.domain.NodeHandler;
import org.chovy.canvas.execution.domain.NodeHandlerRegistry;
import org.springframework.stereotype.Service;

/**
 * 定义 CanvasExecutionApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class CanvasExecutionApplicationService implements CanvasExecutionFacade {

    /**
     * 保存 dagRuntimeService 对应的状态或配置。
     */
    private final DagRuntimeService dagRuntimeService;

    /**
     * 保存 handlerRegistry 对应的状态或配置。
     */
    private final NodeHandlerRegistry handlerRegistry;

    /**
     * 保存 traceService 对应的状态或配置。
     */
    private final ExecutionTraceService traceService;

    /**
     * 保存 definitionRepository 对应的状态或配置。
     */
    private final ExecutionDefinitionRepository definitionRepository;

    public CanvasExecutionApplicationService(
            DagRuntimeService dagRuntimeService,
            NodeHandlerRegistry handlerRegistry,
            ExecutionTraceService traceService,
            ExecutionDefinitionRepository definitionRepository) {
        this.dagRuntimeService = dagRuntimeService;
        this.handlerRegistry = handlerRegistry;
        this.traceService = traceService;
        this.definitionRepository = definitionRepository;
    }

    /**
     * 执行 trigger 对应的业务处理。
     * @param command command 参数
     * @return 处理后的结果
     */
    @Override
    public ExecutionResultView trigger(ExecutionRequestCommand command) {
        PublishedCanvasDefinition definition = definitionRepository.getPublished(command.tenantId(), command.canvasId());
        DagGraph graph = dagRuntimeService.validate(definition);
        String executionId = UUID.randomUUID().toString();
        Long versionId = command.versionId() == null ? definition.versionId() : command.versionId();
        traceService.start(command.tenantId(), executionId, command.canvasId(), versionId);

        Map<String, Object> contextData = new LinkedHashMap<>(command.payload());
        Queue<String> queue = new ArrayDeque<>(graph.entryNodes());
        Set<String> executed = new LinkedHashSet<>();
        Set<String> completed = new LinkedHashSet<>();
        try {
            while (!queue.isEmpty()) {
                String nodeId = queue.remove();
                // 同一执行轮次内只运行一次节点，避免分支汇聚时重复触发副作用。
                if (!executed.add(nodeId)) {
                    continue;
                }
                DagNode node = graph.node(nodeId);
                NodeHandler handler = handlerRegistry.handler(node.nodeType());
                NodeExecutionResult result = handler.execute(new NodeExecutionContext(
                        executionId,
                        node,
                        command.userId(),
                        command.payload(),
                        contextData));
                contextData.putAll(result.output());
                if (!result.success()) {
                    // 失败节点会立即终止整次执行，并把失败原因写入主执行轨迹。
                    rememberNodeResult(contextData, nodeId, "FAILED", result.output());
                    traceService.recordNode(command.tenantId(), executionId, nodeId, node.nodeType(),
                            "FAILED", result.error(), result.output());
                    traceService.finish(command.tenantId(), executionId, "FAILED", result.error());
                    return new ExecutionResultView(executionId, "FAILED");
                }
                if (result.pending()) {
                    // WAIT 类节点暂停后续调度，等待外部事件恢复同一个执行上下文。
                    rememberNodeResult(contextData, nodeId, "WAITING", result.output());
                    traceService.recordNode(command.tenantId(), executionId, nodeId, node.nodeType(),
                            "WAITING", "", result.output());
                    traceService.finish(command.tenantId(), executionId, "PAUSED", "");
                    return new ExecutionResultView(executionId, "PAUSED");
                }
                rememberNodeResult(contextData, nodeId, "SUCCESS", result.output());
                traceService.recordNode(command.tenantId(), executionId, nodeId, node.nodeType(),
                        "SUCCESS", "", result.output());
                completed.add(nodeId);
                enqueueNext(queue, graph, nodeId, result, completed);
            }
            traceService.finish(command.tenantId(), executionId, "SUCCESS", "");
            return new ExecutionResultView(executionId, "SUCCESS");
        } catch (RuntimeException e) {
            traceService.finish(command.tenantId(), executionId, "FAILED", e.getMessage());
            throw e;
        }
    }

    /**
     * 执行 trace 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @return 处理后的结果
     */
    @Override
    public ExecutionTraceView trace(Long tenantId, String executionId) {
        return traceService.trace(tenantId, executionId);
    }

    private void enqueueNext(
            Queue<String> queue,
            DagGraph graph,
            String nodeId,
            NodeExecutionResult result,
            Set<String> completed) {
        if (!result.routes().isEmpty()) {
            // 条件节点显式给出路由时，只调度被命中的目标节点。
            result.routes().values().stream()
                    .filter(target -> target != null && !target.isBlank())
                    .filter(target -> readyToRun(graph, target, completed))
                    .forEach(queue::add);
            return;
        }
        graph.downstream(nodeId).stream()
                .filter(target -> readyToRun(graph, target, completed))
                .forEach(queue::add);
    }

    /**
     * 执行 readyToRun 对应的业务处理。
     * @param graph graph 参数
     * @param nodeId nodeId 参数
     * @param completed completed 参数
     */
    private boolean readyToRun(DagGraph graph, String nodeId, Set<String> completed) {
        DagNode node = graph.node(nodeId);
        if (node == null || !"AGGREGATE".equals(node.nodeType())) {
            return true;
        }
        // 聚合节点必须等待配置的上游集合全部完成后才能入队。
        Set<String> requiredUpstream = new LinkedHashSet<>(stringList(node.config().get("upstreamIds")));
        if (requiredUpstream.isEmpty()) {
            requiredUpstream.addAll(graph.upstream(nodeId));
        }
        return completed.containsAll(requiredUpstream);
    }

    /**
     * 执行 stringList 对应的业务处理。
     * @param value value 参数
     * @return 处理后的结果
     */
    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        return rawList.stream()
                .map(item -> item == null ? "" : String.valueOf(item).trim())
                .filter(item -> !item.isBlank())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private void rememberNodeResult(
            Map<String, Object> contextData,
            String nodeId,
            String status,
            Map<String, Object> output) {
        Map<String, Object> nodeOutputs = new LinkedHashMap<>();
        Object existingOutputs = contextData.get("nodeOutputs");
        if (existingOutputs instanceof Map<?, ?> map) {
            nodeOutputs.putAll((Map<String, Object>) map);
        }
        nodeOutputs.put(nodeId, output);
        contextData.put("nodeOutputs", Map.copyOf(nodeOutputs));

        Map<String, Object> nodeStatuses = new LinkedHashMap<>();
        Object existingStatuses = contextData.get("nodeStatuses");
        if (existingStatuses instanceof Map<?, ?> map) {
            nodeStatuses.putAll((Map<String, Object>) map);
        }
        nodeStatuses.put(nodeId, status);
        contextData.put("nodeStatuses", Map.copyOf(nodeStatuses));
    }
}
