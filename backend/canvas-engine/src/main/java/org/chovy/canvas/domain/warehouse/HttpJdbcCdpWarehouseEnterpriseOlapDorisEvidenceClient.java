package org.chovy.canvas.domain.warehouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient 编排 domain.warehouse 场景的领域业务规则。
 */
@Service
public class HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient
        implements CdpWarehouseEnterpriseOlapDorisEvidenceClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final ObjectProvider<JdbcTemplate> dorisJdbcTemplate;
    private final CdpWarehouseDorisPrometheusMetricsParser parser;
    private final MetricsFetcher metricsFetcher;
    private final List<String> feMetricsUrls;
    private final List<String> beMetricsUrls;
    private final Duration timeout;
    private final String querySloSql;

    /**
     * 创建 HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient 实例并注入 domain.warehouse 场景依赖。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient 流程中的校验、计算或对象转换。
     * @param feMetricsUrls fe metrics urls 参数，用于 HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient 流程中的校验、计算或对象转换。
     * @param beMetricsUrls be metrics urls 参数，用于 HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient 流程中的校验、计算或对象转换。
     * @param querySloSql query slo sql 参数，用于 HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient 流程中的校验、计算或对象转换。
     */
    @Autowired
    public HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient(
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            @Value("${canvas.doris.fe-metrics-urls:}") String feMetricsUrls,
            @Value("${canvas.doris.be-metrics-urls:}") String beMetricsUrls,
            @Value("${canvas.doris.query-slo-sql:}") String querySloSql) {
        this(dorisJdbcTemplate,
                new CdpWarehouseDorisPrometheusMetricsParser(),
                new JavaHttpMetricsFetcher(HttpClient.newBuilder()
                        .connectTimeout(DEFAULT_TIMEOUT)
                        .build()),
                splitUrls(feMetricsUrls),
                splitUrls(beMetricsUrls),
                DEFAULT_TIMEOUT,
                querySloSql);
    }

    /**
     * 执行 HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient 流程，围绕 http jdbc cdp warehouse enterprise olap doris evidence client 完成校验、计算或结果组装。
     *
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient 流程中的校验、计算或对象转换。
     * @param parser parser 参数，用于 HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient 流程中的校验、计算或对象转换。
     * @param metricsFetcher metrics fetcher 参数，用于 HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient 流程中的校验、计算或对象转换。
     * @param feMetricsUrls fe metrics urls 参数，用于 HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient 流程中的校验、计算或对象转换。
     * @param beMetricsUrls be metrics urls 参数，用于 HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient 流程中的校验、计算或对象转换。
     * @param timeout 时间参数，用于计算窗口、过期或审计时间。
     * @param querySloSql query slo sql 参数，用于 HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient 流程中的校验、计算或对象转换。
     */
    HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient(
            ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            CdpWarehouseDorisPrometheusMetricsParser parser,
            MetricsFetcher metricsFetcher,
            List<String> feMetricsUrls,
            List<String> beMetricsUrls,
            Duration timeout,
            String querySloSql) {
        this.dorisJdbcTemplate = dorisJdbcTemplate;
        this.parser = parser == null ? new CdpWarehouseDorisPrometheusMetricsParser() : parser;
        this.metricsFetcher = metricsFetcher;
        this.feMetricsUrls = feMetricsUrls == null ? List.of() : List.copyOf(feMetricsUrls);
        this.beMetricsUrls = beMetricsUrls == null ? List.of() : List.copyOf(beMetricsUrls);
        this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        this.querySloSql = querySloSql == null ? "" : querySloSql.trim();
    }

    /**
     * 拉取 Doris FE 和 BE 的 Prometheus 指标证据。
     *
     * <p>方法会访问配置的 FE/BE metrics URL，解析 Prometheus 文本并聚合为带采集时间的端点证据。
     * 配置缺失或任一端点拉取失败会抛出异常，调用方据此判定本轮证据采集失败。</p>
     *
     * @return 当前采集时刻的 Doris 指标端点证据集合
     */
    @Override
    public DorisMetricsEvidence metrics() {
        if (feMetricsUrls.isEmpty() || beMetricsUrls.isEmpty()) {
            throw new IllegalStateException("Doris FE and BE metrics URLs must both be configured");
        }
        List<MetricsEndpointEvidence> endpoints = new ArrayList<>();
        collectMetrics("FE", feMetricsUrls, endpoints);
        collectMetrics("BE", beMetricsUrls, endpoints);
        return new DorisMetricsEvidence(LocalDateTime.now(), endpoints);
    }

    /**
     * 通过 Doris JDBC 查询工作负载组配置。
     *
     * <p>方法读取 {@code information_schema.workload_groups} 并转换 CPU、内存、并发、队列和吞吐限制字段；
     * 未配置 Doris JDBC 时抛出异常，不返回模拟数据。</p>
     *
     * @return Doris 当前工作负载组配置证据列表，数据库无记录时为空列表
     */
    @Override
    public List<WorkloadGroupEvidence> workloadGroups() {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        JdbcTemplate jdbcTemplate = dorisJdbcTemplate == null ? null : dorisJdbcTemplate.getIfAvailable();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (jdbcTemplate == null) {
            throw new IllegalStateException("Doris JDBC is not configured");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT name,
                       min_cpu_percent,
                       max_cpu_percent,
                       min_memory_percent,
                       max_memory_percent,
                       max_concurrency,
                       max_queue_size,
                       queue_timeout,
                       read_bytes_per_second,
                       remote_read_bytes_per_second
                FROM information_schema.workload_groups
                """);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return (rows == null ? List.<Map<String, Object>>of() : rows).stream()
                .map(this::workloadGroup)
                .toList();
    }

    /**
     * 执行配置 SQL 查询 Doris 查询 SLO 证据。
     *
     * <p>方法只执行配置项中的只读 SLO SQL，并把 profile、工作负载组、延迟、错误数和内存峰值等列转换为证据对象。
     * SQL 或 Doris JDBC 未配置时抛出异常。</p>
     *
     * @return 查询 SLO 采样证据列表，SQL 无结果时为空列表
     */
    @Override
    public List<QuerySloEvidence> querySlo() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (querySloSql.isBlank()) {
            throw new IllegalStateException("Doris query SLO SQL is not configured");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        JdbcTemplate jdbcTemplate = dorisJdbcTemplate == null ? null : dorisJdbcTemplate.getIfAvailable();
        if (jdbcTemplate == null) {
            throw new IllegalStateException("Doris JDBC is not configured");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(querySloSql);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return (rows == null ? List.<Map<String, Object>>of() : rows).stream()
                .map(this::querySlo)
                .toList();
    }

    /**
     * 执行 collectMetrics 流程，围绕 collect metrics 完成校验、计算或结果组装。
     *
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param urls urls 参数，用于 collectMetrics 流程中的校验、计算或对象转换。
     * @param endpoints endpoints 参数，用于 collectMetrics 流程中的校验、计算或对象转换。
     */
    private void collectMetrics(String role, List<String> urls, List<MetricsEndpointEvidence> endpoints) {
        for (String url : urls) {
            try {
                LocalDateTime measuredAt = LocalDateTime.now();
                CdpWarehouseDorisPrometheusMetricsParser.ParsedMetrics parsed =
                        parser.parse(url, role, metricsFetcher.fetch(url, timeout), measuredAt);
                endpoints.add(new MetricsEndpointEvidence(
                        parsed.endpoint(),
                        parsed.role(),
                        parsed.measuredAt(),
                        parsed.metrics()));
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (RuntimeException e) {
                throw e;
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (Exception e) {
                throw new IllegalStateException("Doris " + role + " metrics fetch failed for " + url
                        + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * 执行 workloadGroup 流程，围绕 workload group 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 workloadGroup 流程中的校验、计算或对象转换。
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 workloadGroup 流程生成的业务结果。
     */
    private WorkloadGroupEvidence workloadGroup(Map<String, Object> row) {
        return new WorkloadGroupEvidence(
                string(row, "name"),
                doubleValue(row, "min_cpu_percent"),
                doubleValue(row, "max_cpu_percent"),
                doubleValue(row, "min_memory_percent"),
                doubleValue(row, "max_memory_percent"),
                intValue(row, "max_concurrency"),
                intValue(row, "max_queue_size"),
                longValue(row, "queue_timeout"),
                longValue(row, "read_bytes_per_second"),
                longValue(row, "remote_read_bytes_per_second"));
    }

    /**
     * 查询或读取业务数据。
     *
     * @param String string 参数，用于 querySlo 流程中的校验、计算或对象转换。
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回符合条件的数据列表或视图。
     */
    private QuerySloEvidence querySlo(Map<String, Object> row) {
        return new QuerySloEvidence(
                string(row, "profile_key"),
                string(row, "workload_group"),
                longValue(row, "sample_count"),
                longValue(row, "error_count"),
                doubleValue(row, "p95_latency_ms"),
                doubleValue(row, "p99_latency_ms"),
                doubleValue(row, "max_queue_wait_ms"),
                longValue(row, "max_peak_memory_bytes"),
                localDateTimeValue(row, "measured_at"));
    }

    /**
     * 执行 string 流程，围绕 string 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 string 流程中的校验、计算或对象转换。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 string 生成的文本或业务键。
     */
    private String string(Map<String, Object> row, String key) {
        Object value = value(row, key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 执行 doubleValue 流程，围绕 double value 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 doubleValue 流程中的校验、计算或对象转换。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 double value 计算得到的数量、金额或指标值。
     */
    private Double doubleValue(Map<String, Object> row, String key) {
        Object value = value(row, key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        String raw = String.valueOf(value).replace("%", "").trim();
        try {
            return Double.parseDouble(raw);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 执行 intValue 流程，围绕 int value 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 intValue 流程中的校验、计算或对象转换。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 int value 计算得到的数量、金额或指标值。
     */
    private Integer intValue(Map<String, Object> row, String key) {
        Long value = longValue(row, key);
        return value == null ? null : Math.toIntExact(Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, value)));
    }

    /**
     * 执行 longValue 流程，围绕 long value 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 longValue 流程中的校验、计算或对象转换。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 long value 计算得到的数量、金额或指标值。
     */
    private Long longValue(Map<String, Object> row, String key) {
        Object value = value(row, key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 执行 localDateTimeValue 流程，围绕 local date time value 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 localDateTimeValue 流程中的校验、计算或对象转换。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 localDateTimeValue 流程生成的业务结果。
     */
    private LocalDateTime localDateTimeValue(Map<String, Object> row, String key) {
        // 准备本次处理所需的上下文和中间变量。
        Object value = value(row, key);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return LocalDateTime.parse(String.valueOf(value).trim().replace(' ', 'T'));
    }

    /**
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 value 流程中的校验、计算或对象转换。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 value 流程生成的业务结果。
     */
    private Object value(Map<String, Object> row, String key) {
        if (row == null) {
            return null;
        }
        Object direct = row.get(key);
        if (direct != null) {
            return direct;
        }
        return row.get(key.toUpperCase(Locale.ROOT));
    }

    /**
     * 执行 splitUrls 流程，围绕 split urls 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 split urls 汇总后的集合、分页或映射视图。
     */
    private static List<String> splitUrls(String value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                urls.add(part.trim());
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(urls);
    }

    /**
     * MetricsFetcher 接口契约。
     */
    interface MetricsFetcher {
        /**
         * 执行 fetch 流程，围绕 fetch 完成校验、计算或结果组装。
         *
         * @param url url 参数，用于 fetch 流程中的校验、计算或对象转换。
         * @param timeout 时间参数，用于计算窗口、过期或审计时间。
         * @return 返回 fetch 生成的文本或业务键。
         */
        String fetch(String url, Duration timeout) throws IOException, InterruptedException;
    }

    /**
     * JavaHttpMetricsFetcher 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static class JavaHttpMetricsFetcher implements MetricsFetcher {
        private final HttpClient httpClient;

        /**
         * 初始化 JavaHttpMetricsFetcher 实例。
         *
         * @param httpClient 依赖组件，用于完成数据访问或外部能力调用。
         */
        JavaHttpMetricsFetcher(HttpClient httpClient) {
            this.httpClient = httpClient;
        }

        /**
         * 执行一次 Prometheus metrics HTTP GET。
         *
         * <p>该回调由外层采集流程调用，负责按传入超时时间请求目标 URL；非 2xx 响应会作为 IO 异常返回给调用方，
         * 不在此处解析 Prometheus 内容。</p>
         *
         * @param url metrics 端点完整地址
         * @param timeout 本次 HTTP 请求的超时时间
         * @return 端点返回的原始 metrics 文本
         * @throws IOException HTTP 状态异常或网络读取失败
         * @throws InterruptedException 请求线程被中断
         */
        @Override
        public String fetch(String url, Duration timeout) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode());
            }
            return response.body();
        }
    }
}
