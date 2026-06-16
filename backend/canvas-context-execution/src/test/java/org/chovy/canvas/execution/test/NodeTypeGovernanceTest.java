package org.chovy.canvas.execution.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 定义 NodeTypeGovernanceTest 的执行上下文数据结构或业务契约。
 */
class NodeTypeGovernanceTest {

    /**
     * 执行 executionRuntimeUsesLocalNodeTypeStringsInsteadOfCommonBusinessEnum 对应的业务处理。
     */
    @Test
    void executionRuntimeUsesLocalNodeTypeStringsInsteadOfCommonBusinessEnum() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        String source = Files.walk(sourceRoot)
                .filter(path -> path.toString().endsWith(".java"))
                .map(NodeTypeGovernanceTest::read)
                .reduce("", String::concat);

        assertThat(source).doesNotContain("org.chovy.canvas.common.enums.NodeType");
    }

    /**
     * 执行 read 对应的业务处理。
     * @param path path 参数
     * @return 处理后的结果
     */
    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
