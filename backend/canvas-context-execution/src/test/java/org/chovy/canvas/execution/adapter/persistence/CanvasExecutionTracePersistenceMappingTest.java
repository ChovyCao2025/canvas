package org.chovy.canvas.execution.adapter.persistence;

import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTable;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableField;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableId;

import com.baomidou.mybatisplus.annotation.IdType;
import org.junit.jupiter.api.Test;

/**
 * 定义 CanvasExecutionTracePersistenceMappingTest 的执行上下文数据结构或业务契约。
 */
class CanvasExecutionTracePersistenceMappingTest {

    /**
     * 执行 mapsCanvasExecutionTraceTableAndTenantColumn 对应的业务处理。
     */
    @Test
    void mapsCanvasExecutionTraceTableAndTenantColumn() {
        assertTable(CanvasExecutionTraceDO.class, "canvas_execution_trace");
        assertTableId(CanvasExecutionTraceDO.class, "id", IdType.AUTO);
        assertTableField(CanvasExecutionTraceDO.class, "tenantId", "tenant_id");
    }
}
