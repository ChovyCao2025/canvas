package org.chovy.canvas.flink;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasFlinkModulePackagingTest {

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
