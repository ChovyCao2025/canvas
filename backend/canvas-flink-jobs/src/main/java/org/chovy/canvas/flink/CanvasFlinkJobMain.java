package org.chovy.canvas.flink;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Flink SQL 作业的命令行启动入口。
 *
 * <p>入口负责解析运行环境、选择 SQL asset、提交 SQL，并把启动提交或失败信息上报为 checkpoint 证据。
 */
public final class CanvasFlinkJobMain {

    /**
     * 启动阶段 checkpoint 使用的虚拟分区。
     */
    private static final String STARTUP_PARTITION = "job-startup";

    /**
     * 启动阶段 checkpoint 使用的虚拟 offset。
     */
    private static final String STARTUP_OFFSET = "submitted";

    /**
     * 启动提交成功时的状态，表示作业已提交但还不是运行期成功证据。
     */
    private static final String STARTUP_STATUS = "WARN";

    /**
     * 启动阶段 checkpoint 的说明文案。
     */
    private static final String STARTUP_MESSAGE = "startup submission is not runtime checkpoint evidence";

    /**
     * 失败原因写入 checkpoint 前允许保留的最大字符数。
     */
    private static final int MAX_ERROR_LENGTH = 1000;

    /**
     * 工具入口类不允许实例化。
     */
    private CanvasFlinkJobMain() {
    }

    /**
     * Flink SQL 作业启动入口。
     *
     * <p>入口会合并环境变量和命令行管道参数，渲染对应 SQL asset，提交 Flink SQL，并上报启动提交 checkpoint。
     * <p>提交失败时会同步上报 FAIL 事件后重新抛出异常，让 Flink/调度系统感知启动失败。
     *
     * @param args 可传 {@code --pipeline-key=<key>} 或第一个非选项参数作为管道标识
     */
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

    /**
     * 执行 Flink SQL 作业启动流程。
     *
     * @param env 环境变量和命令行参数合并后的配置
     * @param executor SQL 执行器
     * @param checkpointSink checkpoint 上报出口
     * @param nowSupplier 当前时间供应器，测试中可固定时间
     * @return 作业启动结果
     */
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
            // SQL 提交失败也要尽力上报，便于平台侧区分未启动和启动失败。
            reportFailure(config, checkpointSink, nowSupplier, ex);
            throw ex;
        }
    }

    /**
     * 合并环境变量和命令行参数中的管道标识。
     *
     * @param env 原始环境变量映射
     * @param args 命令行参数
     * @return 已覆盖 pipeline key 的环境变量副本
     */
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
                // 兼容只传第一个位置参数的 Kubernetes Job 启动方式。
                values.put("CANVAS_FLINK_JOB_PIPELINE_KEY", arg);
            }
        }
        return values;
    }

    /**
     * 在作业启动失败时尽力上报 FAIL checkpoint。
     *
     * @param config 作业配置
     * @param checkpointSink checkpoint 上报出口
     * @param nowSupplier 当前时间供应器
     * @param ex 原始启动异常
     */
    private static void reportFailure(CanvasFlinkJobConfig config,
                                      CheckpointSink checkpointSink,
                                      Supplier<String> nowSupplier,
                                      RuntimeException ex) {
        try {
            checkpointSink.report(payload(config, "FAIL", limit(ex.getMessage()), nowSupplier.get()));
        } catch (RuntimeException reportEx) {
            // 保留原始 SQL 启动失败作为主异常，checkpoint 上报失败只作为附加诊断。
            ex.addSuppressed(reportEx);
        }
    }

    /**
     * 构建启动阶段 checkpoint payload。
     *
     * @param config 作业配置
     * @param status checkpoint 状态
     * @param errorMessage 启动说明或失败原因
     * @param now checkpoint 时间字符串
     * @return 可上报的 checkpoint payload
     */
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

    /**
     * 限制 checkpoint 失败信息长度。
     *
     * @param message 原始失败信息
     * @return 不超过最大长度的失败信息
     */
    private static String limit(String message) {
        String value = message == null ? "Flink SQL job failed" : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    /**
     * 将空白字符串转为 null，避免 checkpoint 中写入无意义版本号。
     *
     * @param value 原始版本号
     * @return 非空版本号或 null
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * checkpoint 上报出口。
     */
    @FunctionalInterface
    interface CheckpointSink {
        /**
         * 上报一条 checkpoint payload。
         *
         * @param payload checkpoint 上报内容
         */
        void report(CanvasFlinkCheckpointReporter.CheckpointPayload payload);
    }

    /**
     * Flink SQL 作业启动结果。
     *
     * <p>该结果只代表 SQL 已提交给 Flink，不代表流式作业后续 checkpoint 已经成功。
     */
    public static final class RunResult {

        /**
         * 已启动的管道标识。
         */
        private final String pipelineKey;

        /**
         * 实际加载的 SQL asset 路径。
         */
        private final String assetPath;

        /**
         * 已提交的 SQL 语句数量。
         */
        private final int statementCount;

        /**
         * 创建 Flink SQL 作业启动结果。
         *
         * @param pipelineKey 已启动的管道标识
         * @param assetPath 实际加载的 SQL asset 路径
         * @param statementCount 已提交的 SQL 语句数量
         */
        public RunResult(String pipelineKey, String assetPath, int statementCount) {
            this.pipelineKey = pipelineKey;
            this.assetPath = assetPath;
            this.statementCount = statementCount;
        }

        /**
         * 返回已启动的管道标识。
         *
         * @return 已启动的管道标识
         */
        public String pipelineKey() {
            return pipelineKey;
        }

        /**
         * 返回实际加载的 SQL asset 路径。
         *
         * @return SQL asset 路径
         */
        public String assetPath() {
            return assetPath;
        }

        /**
         * 返回已提交的 SQL 语句数量。
         *
         * @return SQL 语句数量
         */
        public int statementCount() {
            return statementCount;
        }

        /**
         * 按字段值判断两个启动结果是否相同。
         *
         * @param o 待比较对象
         * @return true 表示所有字段相同
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RunResult runResult)) {
                return false;
            }
            return statementCount == runResult.statementCount
                    && Objects.equals(pipelineKey, runResult.pipelineKey)
                    && Objects.equals(assetPath, runResult.assetPath);
        }

        /**
         * 基于所有字段生成 hashCode。
         *
         * @return 字段组合哈希值
         */
        @Override
        public int hashCode() {
            int result = Objects.hashCode(pipelineKey);
            result = 31 * result + Objects.hashCode(assetPath);
            result = 31 * result + Integer.hashCode(statementCount);
            return result;
        }

        /**
         * 返回与原 record 形式一致的调试字符串。
         *
         * @return 字段名和值组成的字符串
         */
        @Override
        public String toString() {
            return "RunResult["
                    + "pipelineKey=" + pipelineKey
                    + ", assetPath=" + assetPath
                    + ", statementCount=" + statementCount
                    + ']';
        }
    }
}
