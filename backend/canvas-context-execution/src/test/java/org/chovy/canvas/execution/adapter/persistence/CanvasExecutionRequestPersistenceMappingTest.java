package org.chovy.canvas.execution.adapter.persistence;

import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTable;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableField;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableId;

import org.junit.jupiter.api.Test;

class CanvasExecutionRequestPersistenceMappingTest {

    @Test
    void mapsCanvasExecutionRequestTableAndTenantColumn() {
        assertTable(CanvasExecutionRequestDO.class, "canvas_execution_request");
        assertTableId(CanvasExecutionRequestDO.class, "id");
        assertTableField(CanvasExecutionRequestDO.class, "tenantId", "tenant_id");
    }
}
