package org.chovy.canvas.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 定义 NodeStatusTest 的执行上下文数据结构或业务契约。
 */
class NodeStatusTest {

    /**
     * 执行 preservesRuntimeStatusVocabulary 对应的业务处理。
     */
    @Test
    void preservesRuntimeStatusVocabulary() {
        assertThat(NodeStatus.values()).extracting(Enum::name)
                .containsExactly("PENDING", "RUNNING", "WAITING", "SUCCESS", "FAILED", "TIMEOUT", "SUPPRESSED", "SKIPPED");
    }
}
