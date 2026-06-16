package org.chovy.canvas.execution.application;

import org.chovy.canvas.execution.api.ExecutionApprovalFacade;
import org.chovy.canvas.execution.domain.ExecutionApprovalCatalog;
import org.springframework.stereotype.Service;

@Service
public class ExecutionApprovalApplicationService implements ExecutionApprovalFacade {

    private final ExecutionApprovalCatalog catalog;

    public ExecutionApprovalApplicationService() {
        this(new ExecutionApprovalCatalog());
    }

    ExecutionApprovalApplicationService(ExecutionApprovalCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public ExecutionApprovalDecision approve(Long tenantId, String executionId, String actor, String role) {
        return catalog.decide(tenantId, executionId, actor, role, null, true);
    }

    @Override
    public ExecutionApprovalDecision reject(Long tenantId, String executionId, String actor, String reason, String role) {
        return catalog.decide(tenantId, executionId, actor, role, reason, false);
    }
}
