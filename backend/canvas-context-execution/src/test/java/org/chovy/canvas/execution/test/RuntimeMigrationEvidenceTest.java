package org.chovy.canvas.execution.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 定义 RuntimeMigrationEvidenceTest 的执行上下文数据结构或业务契约。
 */
class RuntimeMigrationEvidenceTest {

    /**
     * 执行 executionProductionCodeDoesNotImportLegacyEngineOrDalPackages 对应的业务处理。
     */
    @Test
    void executionProductionCodeDoesNotImportLegacyEngineOrDalPackages() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        String source = Files.walk(sourceRoot)
                .filter(path -> path.toString().endsWith(".java"))
                .map(RuntimeMigrationEvidenceTest::read)
                .reduce("", String::concat);

        assertThat(source).doesNotContain("org.chovy.canvas.engine");
        assertThat(source).doesNotContain("org.chovy.canvas.dal");
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
