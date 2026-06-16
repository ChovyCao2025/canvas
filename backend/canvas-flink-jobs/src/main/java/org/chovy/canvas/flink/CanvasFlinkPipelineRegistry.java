package org.chovy.canvas.flink;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 维护当前 Flink job 包内可启动管道与 SQL asset 的静态映射。
 *
 * <p>提交入口通过该注册表限制可运行的 pipelineKey，避免任意外部参数加载未打包或不存在的 SQL 资源。
 */
public final class CanvasFlinkPipelineRegistry {

    /**
     * MySQL CDP 事件日志同步到 Doris ODS 层的管道标识。
     */
    public static final String MYSQL_CDP_EVENT_LOG_TO_DORIS_ODS = "mysql_cdp_event_log_to_doris_ods";

    /**
     * MySQL 画布执行链路同步到 Doris ODS 层的管道标识。
     */
    public static final String MYSQL_CANVAS_TRACE_TO_DORIS_ODS = "mysql_canvas_trace_to_doris_ods";

    /**
     * Doris ODS CDP 事件加工到 DWD 事实表的管道标识。
     */
    public static final String DORIS_ODS_CDP_EVENT_TO_DWD_FACT = "doris_ods_cdp_event_to_dwd_fact";

    /**
     * Doris DWD 用户事件事实加工到 DWS 日指标的管道标识。
     */
    public static final String DORIS_DWD_USER_FACT_TO_DWS_METRIC_DAILY =
            "doris_dwd_user_fact_to_dws_metric_daily";

    /**
     * 风险实时特征计算管道标识。
     */
    public static final String RISK_REALTIME_FEATURES = "risk_realtime_features";

    /**
     * 管道标识到 classpath SQL asset 路径的不可变映射。
     */
    private static final Map<String, String> PIPELINES = pipelines();

    /**
     * 管道注册表为静态工具类，不允许实例化。
     */
    private CanvasFlinkPipelineRegistry() {
    }

    /**
     * 返回所有已注册管道到 SQL asset 的映射。
     *
     * <p>返回值不可变，可用于启动前展示可选管道或测试校验 registry 覆盖范围。
     *
     * @return 管道标识到 classpath SQL asset 路径的映射
     */
    public static Map<String, String> all() {
        return PIPELINES;
    }

    /**
     * 根据管道标识查找 SQL asset 路径。
     *
     * <p>未知管道会直接抛出异常，避免作业以错误 pipelineKey 启动后提交空 SQL 或错误 SQL。
     *
     * @param pipelineKey 作业管道标识
     * @return classpath 下的 SQL asset 路径
     */
    public static String sqlAssetFor(String pipelineKey) {
        String asset = PIPELINES.get(pipelineKey);
        if (asset == null) {
            throw new IllegalArgumentException("unknown pipeline: " + pipelineKey);
        }
        return asset;
    }

    /**
     * 判断管道标识是否已在当前 job 包中注册。
     *
     * <p>该方法只检查本地 registry，不访问 Flink 集群或 SQL asset 内容。
     *
     * @param pipelineKey 作业管道标识
     * @return true 表示可以通过 registry 找到 SQL asset
     */
    public static boolean isKnown(String pipelineKey) {
        return PIPELINES.containsKey(pipelineKey);
    }

    /**
     * 初始化内置管道到 SQL asset 的不可变映射。
     *
     * @return 管道注册表映射
     */
    private static Map<String, String> pipelines() {
        Map<String, String> pipelines = new LinkedHashMap<>();
        // LinkedHashMap 固定迭代顺序，便于测试和部署校验输出保持稳定。
        pipelines.put(MYSQL_CDP_EVENT_LOG_TO_DORIS_ODS,
                "sql/mysql_cdp_event_log_to_doris_ods.sql");
        pipelines.put(MYSQL_CANVAS_TRACE_TO_DORIS_ODS,
                "sql/mysql_canvas_trace_to_doris_ods.sql");
        pipelines.put(DORIS_ODS_CDP_EVENT_TO_DWD_FACT,
                "sql/doris_ods_cdp_event_to_dwd_fact.sql");
        pipelines.put(DORIS_DWD_USER_FACT_TO_DWS_METRIC_DAILY,
                "sql/doris_dwd_user_fact_to_dws_metric_daily.sql");
        pipelines.put(RISK_REALTIME_FEATURES, "sql/risk_realtime_features.sql");
        return Map.copyOf(pipelines);
    }
}
