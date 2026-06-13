package org.chovy.canvas.execution.adapter.persistence;

import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTable;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableField;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableId;

import org.junit.jupiter.api.Test;

class CanvasExecutionPersistenceMappingTest {

    @Test
    void mapsCanvasExecutionTableAndTenantColumn() {
        assertTable(CanvasExecutionDO.class, "canvas_execution");
        assertTableId(CanvasExecutionDO.class, "id");
        assertTableField(CanvasExecutionDO.class, "tenantId", "tenant_id");
    }
}
