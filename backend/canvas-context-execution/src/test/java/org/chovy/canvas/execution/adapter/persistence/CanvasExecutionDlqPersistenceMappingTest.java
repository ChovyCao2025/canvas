package org.chovy.canvas.execution.adapter.persistence;

import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTable;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableId;

import com.baomidou.mybatisplus.annotation.IdType;
import org.junit.jupiter.api.Test;

/**
 * 定义 CanvasExecutionDlqPersistenceMappingTest 的执行上下文数据结构或业务契约。
 */
class CanvasExecutionDlqPersistenceMappingTest {

    /**
     * 执行 mapsCanvasExecutionDlqTable 对应的业务处理。
     */
    @Test
    void mapsCanvasExecutionDlqTable() {
        assertTable(CanvasExecutionDlqDO.class, "canvas_execution_dlq");
        assertTableId(CanvasExecutionDlqDO.class, "id", IdType.AUTO);
    }
}
