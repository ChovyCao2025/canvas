package org.chovy.canvas.execution.adapter.persistence;

import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTable;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableId;

import com.baomidou.mybatisplus.annotation.IdType;
import org.junit.jupiter.api.Test;

/**
 * 定义 CanvasMqTriggerRejectedPersistenceMappingTest 的执行上下文数据结构或业务契约。
 */
class CanvasMqTriggerRejectedPersistenceMappingTest {

    /**
     * 执行 mapsCanvasMqTriggerRejectedTable 对应的业务处理。
     */
    @Test
    void mapsCanvasMqTriggerRejectedTable() {
        assertTable(CanvasMqTriggerRejectedDO.class, "canvas_mq_trigger_rejected");
        assertTableId(CanvasMqTriggerRejectedDO.class, "id", IdType.AUTO);
    }
}
