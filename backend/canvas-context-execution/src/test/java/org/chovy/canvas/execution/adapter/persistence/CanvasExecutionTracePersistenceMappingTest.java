package org.chovy.canvas.execution.adapter.persistence;

import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTable;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableField;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableId;

import com.baomidou.mybatisplus.annotation.IdType;
import org.junit.jupiter.api.Test;

class CanvasExecutionTracePersistenceMappingTest {

    @Test
    void mapsCanvasExecutionTraceTableAndTenantColumn() {
        assertTable(CanvasExecutionTraceDO.class, "canvas_execution_trace");
        assertTableId(CanvasExecutionTraceDO.class, "id", IdType.AUTO);
        assertTableField(CanvasExecutionTraceDO.class, "tenantId", "tenant_id");
    }
}
