package org.chovy.canvas.execution.adapter.persistence;

import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTable;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableField;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableId;

import org.junit.jupiter.api.Test;

/**
 * 定义 CanvasExecutionPersistenceMappingTest 的执行上下文数据结构或业务契约。
 */
class CanvasExecutionPersistenceMappingTest {

    /**
     * 执行 mapsCanvasExecutionTableAndTenantColumn 对应的业务处理。
     */
    @Test
    void mapsCanvasExecutionTableAndTenantColumn() {
        assertTable(CanvasExecutionDO.class, "canvas_execution");
        assertTableId(CanvasExecutionDO.class, "id");
        assertTableField(CanvasExecutionDO.class, "tenantId", "tenant_id");
    }
}
