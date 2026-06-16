package org.chovy.canvas.execution.api.dryrun;

import java.util.List;
import java.util.Map;

public interface ExecutionDryRunFacade {

    DryRunResultView dryRun(DryRunCommand command);

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
