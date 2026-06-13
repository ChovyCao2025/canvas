package org.chovy.canvas.execution.adapter.persistence;

import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTable;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableId;

import com.baomidou.mybatisplus.annotation.IdType;
import org.junit.jupiter.api.Test;

class CanvasExecutionDlqPersistenceMappingTest {

    @Test
    void mapsCanvasExecutionDlqTable() {
        assertTable(CanvasExecutionDlqDO.class, "canvas_execution_dlq");
        assertTableId(CanvasExecutionDlqDO.class, "id", IdType.AUTO);
    }
}
