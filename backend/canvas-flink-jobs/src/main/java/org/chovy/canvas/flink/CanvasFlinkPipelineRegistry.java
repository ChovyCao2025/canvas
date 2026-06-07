package org.chovy.canvas.flink;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CanvasFlinkPipelineRegistry {

    public static final String MYSQL_CDP_EVENT_LOG_TO_DORIS_ODS = "mysql_cdp_event_log_to_doris_ods";
    public static final String MYSQL_CANVAS_TRACE_TO_DORIS_ODS = "mysql_canvas_trace_to_doris_ods";
    public static final String DORIS_ODS_CDP_EVENT_TO_DWD_FACT = "doris_ods_cdp_event_to_dwd_fact";
    public static final String DORIS_DWD_USER_FACT_TO_DWS_METRIC_DAILY =
            "doris_dwd_user_fact_to_dws_metric_daily";

    private static final Map<String, String> PIPELINES = pipelines();

    private CanvasFlinkPipelineRegistry() {
    }

    public static Map<String, String> all() {
        return PIPELINES;
    }

    public static String sqlAssetFor(String pipelineKey) {
        String asset = PIPELINES.get(pipelineKey);
        if (asset == null) {
            throw new IllegalArgumentException("unknown pipeline: " + pipelineKey);
        }
        return asset;
    }

    public static boolean isKnown(String pipelineKey) {
        return PIPELINES.containsKey(pipelineKey);
    }

    private static Map<String, String> pipelines() {
        Map<String, String> pipelines = new LinkedHashMap<>();
        pipelines.put(MYSQL_CDP_EVENT_LOG_TO_DORIS_ODS,
                "sql/mysql_cdp_event_log_to_doris_ods.sql");
        pipelines.put(MYSQL_CANVAS_TRACE_TO_DORIS_ODS,
                "sql/mysql_canvas_trace_to_doris_ods.sql");
        pipelines.put(DORIS_ODS_CDP_EVENT_TO_DWD_FACT,
                "sql/doris_ods_cdp_event_to_dwd_fact.sql");
        pipelines.put(DORIS_DWD_USER_FACT_TO_DWS_METRIC_DAILY,
                "sql/doris_dwd_user_fact_to_dws_metric_daily.sql");
        return Map.copyOf(pipelines);
    }
}
