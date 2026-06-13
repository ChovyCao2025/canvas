package org.chovy.canvas.execution.adapter.persistence;

import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTable;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableId;

import com.baomidou.mybatisplus.annotation.IdType;
import org.junit.jupiter.api.Test;

class CanvasExecutionStatsPersistenceMappingTest {

    @Test
    void mapsCanvasExecutionStatsTable() {
        assertTable(CanvasExecutionStatsDO.class, "canvas_execution_stats");
        assertTableId(CanvasExecutionStatsDO.class, "id", IdType.AUTO);
    }
}
