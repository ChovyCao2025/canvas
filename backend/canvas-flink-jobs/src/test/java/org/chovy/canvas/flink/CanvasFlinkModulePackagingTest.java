package org.chovy.canvas.flink;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Flink job 模块打包配置包含运行所需的连接器和入口类。
 */
class CanvasFlinkModulePackagingTest {

    /**
     * POM 应把连接器依赖和 main class 配置进可运行 job jar。
     *
     * @throws Exception POM 读取失败时抛出
     */
    @Test
    void pomPackagesConnectorDependenciesIntoRunnableJobJar() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));

        assertThat(pom)
                .contains("<artifactId>flink-sql-connector-mysql-cdc</artifactId>")
                .contains("<artifactId>flink-doris-connector-1.20</artifactId>")
                .contains("<artifactId>maven-shade-plugin</artifactId>")
                .contains("<mainClass>org.chovy.canvas.flink.CanvasFlinkJobMain</mainClass>");
    }
}
