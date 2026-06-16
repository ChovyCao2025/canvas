package org.chovy.canvas.execution.adapter.persistence;

import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTable;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableId;

import com.baomidou.mybatisplus.annotation.IdType;
import org.junit.jupiter.api.Test;

/**
 * 定义 CanvasExecutionStatsPersistenceMappingTest 的执行上下文数据结构或业务契约。
 */
class CanvasExecutionStatsPersistenceMappingTest {

    /**
     * 执行 mapsCanvasExecutionStatsTable 对应的业务处理。
     */
    @Test
    void mapsCanvasExecutionStatsTable() {
        assertTable(CanvasExecutionStatsDO.class, "canvas_execution_stats");
        assertTableId(CanvasExecutionStatsDO.class, "id", IdType.AUTO);
    }
}
