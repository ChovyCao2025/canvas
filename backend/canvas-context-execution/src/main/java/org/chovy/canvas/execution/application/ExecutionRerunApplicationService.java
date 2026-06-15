package org.chovy.canvas.execution.application;

import java.util.List;

import org.chovy.canvas.execution.api.ExecutionRerunFacade;
import org.chovy.canvas.execution.domain.ExecutionRerunCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionRerunApplicationService implements ExecutionRerunFacade {

    private final ExecutionRerunCatalog catalog;

    public ExecutionRerunApplicationService() {
        this(new ExecutionRerunCatalog());
    }

    public ExecutionRerunApplicationService(ExecutionRerunCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RerunResult rerun(Long tenantId, String operator, boolean admin, Long canvasId, RerunCommand command) {
        return catalog.rerun(tenantId, operator, admin, canvasId, command);
    }

    @Override
    public AuditRow audit(Long tenantId, Long id) {
        return catalog.audit(tenantId, id);
    }

    @Override
    public List<AuditRow> audits(Long tenantId, Long canvasId) {
        return catalog.audits(tenantId, canvasId);
    }
}
