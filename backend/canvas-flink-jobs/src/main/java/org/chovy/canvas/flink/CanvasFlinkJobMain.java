package org.chovy.canvas.flink;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class CanvasFlinkJobMain {

    private static final String STARTUP_PARTITION = "job-startup";
    private static final String STARTUP_OFFSET = "submitted";
    private static final String STARTUP_STATUS = "WARN";
    private static final String STARTUP_MESSAGE = "startup submission is not runtime checkpoint evidence";
    private static final int MAX_ERROR_LENGTH = 1000;

    private CanvasFlinkJobMain() {
    }

    public static void main(String[] args) {
        Map<String, String> env = environmentWithArgs(System.getenv(), args);
        CanvasFlinkJobConfig config = CanvasFlinkJobConfig.from(env);
        CanvasFlinkCheckpointReporter reporter = new CanvasFlinkCheckpointReporter(
                config.checkpointEndpoint(), Duration.ofSeconds(5), config.internalApiToken());
        run(env,
                CanvasFlinkSqlJobRunner.flinkExecutor(),
                reporter::report,
                () -> OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
    }

    static RunResult run(Map<String, String> env,
                         CanvasFlinkSqlJobRunner.SqlExecutor executor,
                         CheckpointSink checkpointSink,
                         Supplier<String> nowSupplier) {
        CanvasFlinkJobConfig config = CanvasFlinkJobConfig.from(env);
        String assetPath = CanvasFlinkPipelineRegistry.sqlAssetFor(config.pipelineKey());
        String renderedSql = CanvasFlinkSqlTemplateLoader.render(
                CanvasFlinkSqlTemplateLoader.load(assetPath),
                config.placeholders());
        try {
            int statementCount = CanvasFlinkSqlJobRunner.run(renderedSql, executor);
            checkpointSink.report(payload(config, STARTUP_STATUS, STARTUP_MESSAGE, nowSupplier.get()));
            return new RunResult(config.pipelineKey(), assetPath, statementCount);
        } catch (RuntimeException ex) {
            reportFailure(config, checkpointSink, nowSupplier, ex);
            throw ex;
        }
    }

    static Map<String, String> environmentWithArgs(Map<String, String> env, String[] args) {
        Map<String, String> values = new LinkedHashMap<>(env == null ? Map.of() : env);
        if (args == null) {
            return values;
        }
        for (String arg : args) {
            if (arg == null || arg.isBlank()) {
                continue;
            }
            if (arg.startsWith("--pipeline-key=")) {
                values.put("CANVAS_FLINK_JOB_PIPELINE_KEY", arg.substring("--pipeline-key=".length()));
            } else if (!arg.startsWith("--") && !values.containsKey("CANVAS_FLINK_JOB_PIPELINE_KEY")) {
                values.put("CANVAS_FLINK_JOB_PIPELINE_KEY", arg);
            }
        }
        return values;
    }

    private static void reportFailure(CanvasFlinkJobConfig config,
                                      CheckpointSink checkpointSink,
                                      Supplier<String> nowSupplier,
                                      RuntimeException ex) {
        try {
            checkpointSink.report(payload(config, "FAIL", limit(ex.getMessage()), nowSupplier.get()));
        } catch (RuntimeException reportEx) {
            ex.addSuppressed(reportEx);
        }
    }

    private static CanvasFlinkCheckpointReporter.CheckpointPayload payload(CanvasFlinkJobConfig config,
                                                                           String status,
                                                                           String errorMessage,
                                                                           String now) {
        return new CanvasFlinkCheckpointReporter.CheckpointPayload(
                config.pipelineKey(),
                config.pipelineKey() + "-startup",
                STARTUP_PARTITION,
                STARTUP_OFFSET,
                STARTUP_OFFSET,
                now,
                now,
                0L,
                0L,
                status,
                errorMessage,
                config.reportedBy(),
                blankToNull(config.sourceSchemaVersion()),
                blankToNull(config.sinkSchemaVersion()));
    }

    private static String limit(String message) {
        String value = message == null ? "Flink SQL job failed" : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @FunctionalInterface
    interface CheckpointSink {
        void report(CanvasFlinkCheckpointReporter.CheckpointPayload payload);
    }

    public record RunResult(String pipelineKey, String assetPath, int statementCount) {
    }
}
