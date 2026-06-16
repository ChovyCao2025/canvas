package org.chovy.canvas.execution.adapter.persistence;

import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTable;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableId;

import com.baomidou.mybatisplus.annotation.IdType;
import org.junit.jupiter.api.Test;

class CanvasMqTriggerRejectedPersistenceMappingTest {

    @Test
    void mapsCanvasMqTriggerRejectedTable() {
        assertTable(CanvasMqTriggerRejectedDO.class, "canvas_mq_trigger_rejected");
        assertTableId(CanvasMqTriggerRejectedDO.class, "id", IdType.AUTO);
    }
}
