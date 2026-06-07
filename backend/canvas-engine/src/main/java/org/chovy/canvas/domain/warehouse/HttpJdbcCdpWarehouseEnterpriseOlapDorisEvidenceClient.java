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

    @Override
    public List<WorkloadGroupEvidence> workloadGroups() {
        JdbcTemplate jdbcTemplate = dorisJdbcTemplate == null ? null : dorisJdbcTemplate.getIfAvailable();
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
        return (rows == null ? List.<Map<String, Object>>of() : rows).stream()
                .map(this::workloadGroup)
                .toList();
    }

    @Override
    public List<QuerySloEvidence> querySlo() {
        if (querySloSql.isBlank()) {
            throw new IllegalStateException("Doris query SLO SQL is not configured");
        }
        JdbcTemplate jdbcTemplate = dorisJdbcTemplate == null ? null : dorisJdbcTemplate.getIfAvailable();
        if (jdbcTemplate == null) {
            throw new IllegalStateException("Doris JDBC is not configured");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(querySloSql);
        return (rows == null ? List.<Map<String, Object>>of() : rows).stream()
                .map(this::querySlo)
                .toList();
    }

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
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Doris " + role + " metrics fetch failed for " + url
                        + ": " + e.getMessage(), e);
            }
        }
    }

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

    private String string(Map<String, Object> row, String key) {
        Object value = value(row, key);
        return value == null ? null : String.valueOf(value);
    }

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
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer intValue(Map<String, Object> row, String key) {
        Long value = longValue(row, key);
        return value == null ? null : Math.toIntExact(Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, value)));
    }

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
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDateTime localDateTimeValue(Map<String, Object> row, String key) {
        Object value = value(row, key);
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return LocalDateTime.parse(String.valueOf(value).trim().replace(' ', 'T'));
    }

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

    private static List<String> splitUrls(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                urls.add(part.trim());
            }
        }
        return List.copyOf(urls);
    }

    interface MetricsFetcher {
        String fetch(String url, Duration timeout) throws IOException, InterruptedException;
    }

    private static class JavaHttpMetricsFetcher implements MetricsFetcher {
        private final HttpClient httpClient;

        JavaHttpMetricsFetcher(HttpClient httpClient) {
            this.httpClient = httpClient;
        }

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
