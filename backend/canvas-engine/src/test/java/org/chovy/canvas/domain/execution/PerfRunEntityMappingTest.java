package org.chovy.canvas.domain.execution;

import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;

/**
 * Perf Run Entity Mapping 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class PerfRunEntityMappingTest {

    @Test
    void eventLogExposesPerfRunId() {
        EventLogDO log = new EventLogDO();
        log.setPerfRunId("perf_20260523_001");

        assertThat(log.getPerfRunId()).isEqualTo("perf_20260523_001");
    }

    @Test
    void executionExposesPerfRunId() {
        CanvasExecutionDO execution = new CanvasExecutionDO();
        execution.setPerfRunId("perf_20260523_001");

        assertThat(execution.getPerfRunId()).isEqualTo("perf_20260523_001");
    }

    @Test
    void executionRequestExposesPerfRunId() {
        CanvasExecutionRequestDO request = new CanvasExecutionRequestDO();
        request.setPerfRunId("perf_20260523_001");

        assertThat(request.getPerfRunId()).isEqualTo("perf_20260523_001");
    }

    @Test
    void dlqExposesPerfRunId() {
        CanvasExecutionDlqDO dlq = CanvasExecutionDlqDO.builder()
                .perfRunId("perf_20260523_001")
                .build();

        assertThat(dlq.getPerfRunId()).isEqualTo("perf_20260523_001");
    }
}
