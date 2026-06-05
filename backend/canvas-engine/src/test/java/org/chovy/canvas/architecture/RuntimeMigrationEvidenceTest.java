package org.chovy.canvas.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeMigrationEvidenceTest {

    @Test
    void recordsWebFluxAndMyBatisDependencies() throws IOException {
        String pom = read("pom.xml");

        assertThat(pom).contains("spring-boot-starter-webflux");
        assertThat(pom).contains("mybatis-plus-spring-boot3-starter");
        assertThat(pom).contains("spring-boot-starter-jdbc");
    }

    @Test
    void recordsReactorDagAndDisruptorUsage() throws IOException {
        String dagEngine = read("src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java");
        String disruptor = read("src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java");

        assertThat(dagEngine).contains("reactor.core.publisher.Mono");
        assertThat(dagEngine).contains("NodeGateCoordinator");
        assertThat(dagEngine).contains("NodeTimeoutCoordinator");
        assertThat(disruptor).contains("com.lmax.disruptor");
        assertThat(disruptor).contains("handleEventsWithWorkerPool");
    }

    @Test
    void recordsGroovyScriptHandlerUsage() throws IOException {
        String groovyHandler = read("src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java");
        String groovyExpressionEngine = read("src/main/java/org/chovy/canvas/engine/expression/GroovyExpressionEngine.java");

        assertThat(groovyHandler).contains("ExpressionEngine");
        assertThat(groovyHandler).doesNotContain("SecureASTCustomizer");
        assertThat(groovyHandler).doesNotContain("Executors.newVirtualThreadPerTaskExecutor");
        assertThat(groovyExpressionEngine).contains("SecureASTCustomizer");
        assertThat(groovyExpressionEngine).contains("Executors.newVirtualThreadPerTaskExecutor");
        assertThat(groovyExpressionEngine).contains("GroovyScriptCache");
        assertThat(groovyExpressionEngine).contains("timeoutMs");
    }

    @Test
    void recordsBitmapHashMapping() throws IOException {
        String bitmapStore = read("src/main/java/org/chovy/canvas/engine/audience/AudienceBitmapStore.java");

        assertThat(bitmapStore).contains("Hashing.murmur3_32_fixed");
        assertThat(bitmapStore).contains("Math.abs");
        assertThat(bitmapStore).contains("RoaringBitmap");
        assertThat(bitmapStore).contains("audience:bitmap:");
    }

    @Test
    void recordsTraceMysqlWritePath() throws IOException {
        String traceBuffer = read("src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java");

        assertThat(traceBuffer).contains("CanvasExecutionTraceMapper");
        assertThat(traceBuffer).contains("insertBatch");
        assertThat(traceBuffer).contains("DorisStreamLoader");
        assertThat(traceBuffer).contains("MySQL");
    }

    @Test
    void recordsCurrentPackageBoundaries() throws IOException {
        Path sourceRoot = Path.of("src/main/java/org/chovy/canvas");

        assertThat(sourceRoot.resolve("web")).isDirectory();
        assertThat(sourceRoot.resolve("domain")).isDirectory();
        assertThat(sourceRoot.resolve("engine")).isDirectory();
        assertThat(sourceRoot.resolve("infrastructure")).isDirectory();
        assertThat(sourceRoot.resolve("dal")).isDirectory();
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
