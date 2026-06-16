package org.chovy.canvas.execution.api.dryrun;

import java.util.List;
import java.util.Map;

/**
 * 定义 ExecutionDryRunFacade 的执行上下文数据结构或业务契约。
 */
public interface ExecutionDryRunFacade {

    /**
     * 执行 dryRun 对应的业务处理。
     * @param command command 参数
     */
    DryRunResultView dryRun(DryRunCommand command);

    /**
     * 定义 DryRunCommand 的执行上下文数据结构或业务契约。
     * @param tenantId tenantId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param versionId versionId 对应的数据字段
     * @param payloadJson payloadJson 对应的数据字段
     * @param mockMode mockMode 对应的数据字段
     */
    record DryRunCommand(
            Long tenantId,
            Long canvasId,
            Long versionId,
            String payloadJson,
            boolean mockMode) {

        public DryRunCommand {
            if (tenantId == null || tenantId <= 0) {
                throw new IllegalArgumentException("tenantId is required");
            }
            if (canvasId == null || canvasId <= 0) {
                throw new IllegalArgumentException("canvasId is required");
            }
            payloadJson = payloadJson == null ? "{}" : payloadJson;
        }
    }

    /**
     * 定义 DryRunResultView 的执行上下文数据结构或业务契约。
     * @param executionId executionId 对应的数据字段
     * @param published published 对应的数据字段
     * @param trace trace 对应的数据字段
     * @param matchedNodeIds matchedNodeIds 对应的数据字段
     */
    record DryRunResultView(
            String executionId,
            boolean published,
            Map<String, Object> trace,
            List<String> matchedNodeIds) {

        public DryRunResultView {
            if (executionId == null || executionId.isBlank()) {
                throw new IllegalArgumentException("executionId is required");
            }
            trace = Map.copyOf(trace == null ? Map.of() : trace);
            matchedNodeIds = List.copyOf(matchedNodeIds == null ? List.of() : matchedNodeIds);
        }
    }
}
