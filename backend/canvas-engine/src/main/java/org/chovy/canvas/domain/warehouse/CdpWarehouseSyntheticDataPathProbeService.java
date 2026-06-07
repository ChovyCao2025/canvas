package org.chovy.canvas.domain.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseSyntheticDataPathProbeRunDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseSyntheticDataPathProbeRunMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CdpWarehouseSyntheticDataPathProbeService {

    private static final String DEFAULT_PROBE_KEY = "synthetic_ods";
    private static final String DEFAULT_EVENT_CODE = "__warehouse_probe__";
    private static final String SOURCE_DIRECT_SINK = "DIRECT_SINK";
    private static final String SOURCE_MYSQL_CDC = "MYSQL_CDC";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final int DEFAULT_VERIFY_ATTEMPTS = 3;
    private static final int DEFAULT_VERIFY_DELAY_MS = 100;
    private static final int MAX_VERIFY_ATTEMPTS = 10;
    private static final int MAX_VERIFY_DELAY_MS = 5_000;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final CdpWarehouseSyntheticDataPathProbeRunMapper runMapper;
    private final ObjectProvider<CdpWarehouseEventSink> eventSinkProvider;
    private final ObjectProvider<JdbcTemplate> dorisJdbcTemplate;
    private final ObjectProvider<CdpEventLogMapper> sourceEventLogMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public CdpWarehouseSyntheticDataPathProbeService(
            CdpWarehouseSyntheticDataPathProbeRunMapper runMapper,
            ObjectProvider<CdpWarehouseEventSink> eventSinkProvider,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            ObjectProvider<CdpEventLogMapper> sourceEventLogMapper,
            ObjectMapper objectMapper) {
        this(runMapper, eventSinkProvider, dorisJdbcTemplate, sourceEventLogMapper,
                objectMapper, Clock.systemDefaultZone());
    }

    public CdpWarehouseSyntheticDataPathProbeService(
            CdpWarehouseSyntheticDataPathProbeRunMapper runMapper,
            ObjectProvider<CdpWarehouseEventSink> eventSinkProvider,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            ObjectMapper objectMapper) {
        this(runMapper, eventSinkProvider, dorisJdbcTemplate, null, objectMapper, Clock.systemDefaultZone());
    }

    CdpWarehouseSyntheticDataPathProbeService(
            CdpWarehouseSyntheticDataPathProbeRunMapper runMapper,
            ObjectProvider<CdpWarehouseEventSink> eventSinkProvider,
            ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            ObjectProvider<CdpEventLogMapper> sourceEventLogMapper,
            ObjectMapper objectMapper,
            Clock clock) {
        this.runMapper = runMapper;
        this.eventSinkProvider = eventSinkProvider;
        this.dorisJdbcTemplate = dorisJdbcTemplate;
        this.sourceEventLogMapper = sourceEventLogMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock;
    }

    public ProbeRunView run(Long tenantId, RunCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        RunCommand scopedCommand = command == null
                ? new RunCommand(null, null, true, DEFAULT_VERIFY_ATTEMPTS, DEFAULT_VERIFY_DELAY_MS, null)
                : command;
        boolean strict = !Boolean.FALSE.equals(scopedCommand.strict());
        String probeKey = defaultString(scopedCommand.probeKey(), DEFAULT_PROBE_KEY);
        String eventCode = eventCode(scopedCommand.eventCode());
        String sourceMode = sourceMode(scopedCommand.sourceMode());
        String messageId = "warehouse-probe-" + UUID.randomUUID();
        String userId = "__warehouse_probe_user_" + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime startedAt = now();

        CdpWarehouseSyntheticDataPathProbeRunDO row =
                newRun(scopedTenantId, probeKey, sourceMode, messageId, eventCode, userId, strict, startedAt);
        runMapper.insert(row);

        List<StepEvidence> evidence = new ArrayList<>();
        JdbcTemplate doris = dorisJdbcTemplate == null ? null : dorisJdbcTemplate.getIfAvailable();
        if (doris == null) {
            evidence.add(new StepEvidence("doris_jdbc", strict ? STATUS_FAIL : STATUS_WARN,
                    "Doris JDBC is not configured"));
            if (SOURCE_MYSQL_CDC.equals(sourceMode)) {
                evidence.add(new StepEvidence("source_mysql_write", STATUS_SKIPPED,
                        "source write skipped because ODS read verification is unavailable"));
            } else {
                evidence.add(new StepEvidence("sink_write", STATUS_SKIPPED,
                        "sink write skipped because ODS read verification is unavailable"));
            }
            finish(row, strict ? STATUS_FAIL : STATUS_WARN,
                    STATUS_SKIPPED,
                    STATUS_SKIPPED,
                    strict ? STATUS_FAIL : STATUS_WARN, 0L, evidence, "Doris JDBC is not configured");
            return toView(row);
        }

        CdpEventLogDO syntheticEvent = syntheticEvent(scopedTenantId, messageId, eventCode, userId, probeKey, startedAt);
        String sourceStatus = STATUS_SKIPPED;
        String sinkStatus = STATUS_SKIPPED;

        if (SOURCE_MYSQL_CDC.equals(sourceMode)) {
            CdpEventLogMapper sourceMapper =
                    sourceEventLogMapper == null ? null : sourceEventLogMapper.getIfAvailable();
            if (sourceMapper == null) {
                evidence.add(new StepEvidence("source_mysql_write", STATUS_FAIL,
                        "source cdp_event_log writer is not configured"));
                finish(row, STATUS_FAIL, STATUS_FAIL, STATUS_SKIPPED, null, 0L,
                        evidence, "source cdp_event_log writer is not configured");
                return toView(row);
            }
            try {
                sourceMapper.insert(syntheticEvent);
                sourceStatus = STATUS_PASS;
                evidence.add(new StepEvidence("source_mysql_write", STATUS_PASS,
                        "synthetic event inserted into cdp_event_log for Flink CDC"));
                evidence.add(new StepEvidence("sink_write", STATUS_SKIPPED,
                        "direct Doris Stream Load sink skipped for MySQL CDC proof"));
            } catch (RuntimeException ex) {
                String message = limit(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                evidence.add(new StepEvidence("source_mysql_write", STATUS_FAIL,
                        "source cdp_event_log write failed: " + message));
                finish(row, STATUS_FAIL, STATUS_FAIL, STATUS_SKIPPED, null, 0L, evidence, message);
                return toView(row);
            }
        } else {
            CdpWarehouseEventSink sink = eventSinkProvider == null ? null : eventSinkProvider.getIfAvailable();
            if (sink == null) {
                evidence.add(new StepEvidence("source_mysql_write", STATUS_SKIPPED,
                        "source cdp_event_log write skipped for direct sink proof"));
                evidence.add(new StepEvidence("sink_write", STATUS_FAIL, "warehouse event sink is not configured"));
                finish(row, STATUS_FAIL, STATUS_SKIPPED, STATUS_FAIL, null, 0L,
                        evidence, "warehouse event sink is not configured");
                return toView(row);
            }
            try {
                sink.writeAccepted(syntheticEvent);
                sinkStatus = STATUS_PASS;
                evidence.add(new StepEvidence("source_mysql_write", STATUS_SKIPPED,
                        "source cdp_event_log write skipped for direct sink proof"));
                evidence.add(new StepEvidence("sink_write", STATUS_PASS,
                        "synthetic event accepted by warehouse sink"));
            } catch (RuntimeException ex) {
                String message = limit(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                evidence.add(new StepEvidence("source_mysql_write", STATUS_SKIPPED,
                        "source cdp_event_log write skipped for direct sink proof"));
                evidence.add(new StepEvidence("sink_write", STATUS_FAIL, "warehouse sink write failed: " + message));
                finish(row, STATUS_FAIL, STATUS_SKIPPED, STATUS_FAIL, null, 0L, evidence, message);
                return toView(row);
            }
        }

        long odsRows;
        try {
            odsRows = verifyOds(doris, scopedTenantId, messageId, eventCode,
                    boundedAttempts(scopedCommand.verifyAttempts()),
                    boundedDelayMs(scopedCommand.verifyDelayMs()));
        } catch (RuntimeException ex) {
            String message = limit(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            evidence.add(new StepEvidence("ods_read", STATUS_FAIL, "Doris ODS read failed: " + message));
            finish(row, STATUS_FAIL, sourceStatus, sinkStatus, STATUS_FAIL, 0L, evidence, message);
            return toView(row);
        }

        String odsStatus = odsRows > 0 ? STATUS_PASS : (strict ? STATUS_FAIL : STATUS_WARN);
        evidence.add(new StepEvidence("ods_read", odsStatus,
                odsRows > 0
                        ? "Doris ODS contains synthetic event rows=" + odsRows
                        : "Doris ODS did not expose the synthetic event"));
        finish(row, worstStatus(evidence), sourceStatus, sinkStatus, odsStatus, odsRows, evidence,
                odsRows > 0 ? null : "Doris ODS did not expose the synthetic event");
        return toView(row);
    }

    public List<ProbeRunView> recent(Long tenantId, int limit) {
        return safeList(runMapper.listRecent(normalizeTenant(tenantId), boundLimit(limit)))
                .stream()
                .map(this::toView)
                .toList();
    }

    private long verifyOds(JdbcTemplate doris,
                           Long tenantId,
                           String messageId,
                           String eventCode,
                           int attempts,
                           int delayMs) {
        long rows = 0L;
        for (int i = 0; i < attempts; i++) {
            Long count = doris.queryForObject(odsReadSql(), Long.class, tenantId, messageId, eventCode);
            rows = count == null ? 0L : count;
            if (rows > 0 || i == attempts - 1) {
                return rows;
            }
            sleep(delayMs);
        }
        return rows;
    }

    private String odsReadSql() {
        return """
                SELECT COUNT(1)
                FROM canvas_ods.cdp_event_log
                WHERE tenant_id = ?
                  AND message_id = ?
                  AND event_code = ?
                """;
    }

    private CdpEventLogDO syntheticEvent(Long tenantId,
                                         String messageId,
                                         String eventCode,
                                         String userId,
                                         String probeKey,
                                         LocalDateTime time) {
        CdpEventLogDO row = new CdpEventLogDO();
        row.setTenantId(tenantId);
        row.setWriteKeyId(0L);
        row.setMessageId(messageId);
        row.setEventType("track");
        row.setEventCode(eventCode);
        row.setUserId(userId);
        row.setAnonymousId(null);
        row.setSessionId("warehouse-probe");
        row.setDeviceId("warehouse-probe");
        row.setPlatform("warehouse-probe");
        row.setSdkContext("{}");
        row.setProperties(properties(probeKey, messageId));
        row.setIdempotencyKey(messageId);
        row.setEventTime(time);
        row.setSentAt(time);
        row.setReceivedAt(time);
        row.setStatus(CdpEventLogDO.ACCEPTED);
        row.setCreatedAt(time);
        return row;
    }

    private String properties(String probeKey, String messageId) {
        try {
            var node = objectMapper.createObjectNode();
            node.put("synthetic", true);
            node.put("probeKey", probeKey);
            node.put("messageId", messageId);
            node.put("channel", "warehouse_probe");
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{\"synthetic\":true}";
        }
    }

    private CdpWarehouseSyntheticDataPathProbeRunDO newRun(Long tenantId,
                                                           String probeKey,
                                                           String sourceMode,
                                                           String messageId,
                                                           String eventCode,
                                                           String userId,
                                                           boolean strict,
                                                           LocalDateTime startedAt) {
        CdpWarehouseSyntheticDataPathProbeRunDO row = new CdpWarehouseSyntheticDataPathProbeRunDO();
        row.setTenantId(tenantId);
        row.setProbeKey(probeKey);
        row.setSourceMode(sourceMode);
        row.setMessageId(messageId);
        row.setEventCode(eventCode);
        row.setUserId(userId);
        row.setStrictMode(strict ? 1 : 0);
        row.setStatus(STATUS_RUNNING);
        row.setOdsRowCount(0L);
        row.setStartedAt(startedAt);
        return row;
    }

    private void finish(CdpWarehouseSyntheticDataPathProbeRunDO row,
                        String status,
                        String sourceStatus,
                        String sinkStatus,
                        String odsStatus,
                        long odsRowCount,
                        List<StepEvidence> evidence,
                        String errorMessage) {
        row.setStatus(status);
        row.setSourceStatus(sourceStatus);
        row.setSinkStatus(sinkStatus);
        row.setOdsStatus(odsStatus);
        row.setOdsRowCount(odsRowCount);
        row.setFinishedAt(now());
        row.setErrorMessage(limit(errorMessage));
        row.setEvidenceJson(evidenceJson(evidence));
        runMapper.updateCompletion(row);
    }

    private String evidenceJson(List<StepEvidence> evidence) {
        try {
            return objectMapper.writeValueAsString(evidence == null ? List.of() : evidence);
        } catch (Exception e) {
            return "[]";
        }
    }

    private ProbeRunView toView(CdpWarehouseSyntheticDataPathProbeRunDO row) {
        return new ProbeRunView(
                row.getId(),
                row.getTenantId(),
                row.getProbeKey(),
                defaultString(row.getSourceMode(), SOURCE_DIRECT_SINK),
                row.getMessageId(),
                row.getEventCode(),
                row.getUserId(),
                Integer.valueOf(1).equals(row.getStrictMode()),
                row.getStatus(),
                row.getSourceStatus(),
                row.getSinkStatus(),
                row.getOdsStatus(),
                row.getOdsRowCount() == null ? 0L : row.getOdsRowCount(),
                row.getStartedAt(),
                row.getFinishedAt(),
                row.getErrorMessage(),
                row.getEvidenceJson(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private String worstStatus(List<StepEvidence> evidence) {
        if (evidence.stream().map(StepEvidence::status).anyMatch(STATUS_FAIL::equals)) {
            return STATUS_FAIL;
        }
        if (evidence.stream().map(StepEvidence::status).anyMatch(STATUS_WARN::equals)) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    private String eventCode(String value) {
        String eventCode = defaultString(value, DEFAULT_EVENT_CODE);
        if (!eventCode.startsWith("__warehouse_probe")) {
            throw new IllegalArgumentException("eventCode must use reserved __warehouse_probe prefix");
        }
        return eventCode;
    }

    private String sourceMode(String value) {
        String normalized = defaultString(value, SOURCE_DIRECT_SINK)
                .toUpperCase(Locale.ROOT)
                .replace('-', '_');
        if ("FLINK_CDC".equals(normalized) || "MYSQL_SOURCE".equals(normalized)) {
            return SOURCE_MYSQL_CDC;
        }
        if (SOURCE_DIRECT_SINK.equals(normalized) || SOURCE_MYSQL_CDC.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("sourceMode must be DIRECT_SINK or MYSQL_CDC");
    }

    private int boundedAttempts(Integer value) {
        int attempts = value == null || value <= 0 ? DEFAULT_VERIFY_ATTEMPTS : value;
        return Math.min(attempts, MAX_VERIFY_ATTEMPTS);
    }

    private int boundedDelayMs(Integer value) {
        int delay = value == null || value < 0 ? DEFAULT_VERIFY_DELAY_MS : value;
        return Math.min(delay, MAX_VERIFY_DELAY_MS);
    }

    private int boundLimit(int value) {
        int limit = value <= 0 ? 20 : value;
        return Math.min(limit, MAX_LIMIT);
    }

    private void sleep(int delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("synthetic ODS verification interrupted", e);
        }
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String limit(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }

    private List<CdpWarehouseSyntheticDataPathProbeRunDO> safeList(
            List<CdpWarehouseSyntheticDataPathProbeRunDO> rows) {
        return rows == null ? List.of() : rows;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public record RunCommand(
            String probeKey,
            String eventCode,
            Boolean strict,
            Integer verifyAttempts,
            Integer verifyDelayMs,
            String sourceMode) {
    }

    public record ProbeRunView(
            Long id,
            Long tenantId,
            String probeKey,
            String sourceMode,
            String messageId,
            String eventCode,
            String userId,
            boolean strict,
            String status,
            String sourceStatus,
            String sinkStatus,
            String odsStatus,
            long odsRowCount,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            String errorMessage,
            String evidenceJson,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record StepEvidence(
            String key,
            String status,
            String reason) {
        public StepEvidence {
            status = status == null ? STATUS_FAIL : status.trim().toUpperCase(Locale.ROOT);
        }
    }
}
