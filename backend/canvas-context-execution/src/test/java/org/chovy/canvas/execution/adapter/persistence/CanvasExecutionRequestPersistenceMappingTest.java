package org.chovy.canvas.execution.adapter.persistence;

import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTable;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableField;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableId;

import org.junit.jupiter.api.Test;

/**
 * 定义 CanvasExecutionRequestPersistenceMappingTest 的执行上下文数据结构或业务契约。
 */
class CanvasExecutionRequestPersistenceMappingTest {

    /**
     * 执行 mapsCanvasExecutionRequestTableAndTenantColumn 对应的业务处理。
     */
    @Test
    void mapsCanvasExecutionRequestTableAndTenantColumn() {
        assertTable(CanvasExecutionRequestDO.class, "canvas_execution_request");
        assertTableId(CanvasExecutionRequestDO.class, "id");
        assertTableField(CanvasExecutionRequestDO.class, "tenantId", "tenant_id");
    }
}
