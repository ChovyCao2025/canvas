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
/**
 * CdpWarehouseSyntheticDataPathProbeService 承载对应领域的业务规则、流程编排和结果转换。
 */
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
    /**
     * 初始化 CdpWarehouseSyntheticDataPathProbeService 实例。
     *
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventSinkProvider event sink provider 参数，用于 CdpWarehouseSyntheticDataPathProbeService 流程中的校验、计算或对象转换。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 CdpWarehouseSyntheticDataPathProbeService 流程中的校验、计算或对象转换。
     * @param sourceEventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseSyntheticDataPathProbeService(
            CdpWarehouseSyntheticDataPathProbeRunMapper runMapper,
            ObjectProvider<CdpWarehouseEventSink> eventSinkProvider,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            ObjectProvider<CdpEventLogMapper> sourceEventLogMapper,
            ObjectMapper objectMapper) {
        this(runMapper, eventSinkProvider, dorisJdbcTemplate, sourceEventLogMapper,
                objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 初始化 CdpWarehouseSyntheticDataPathProbeService 实例。
     *
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventSinkProvider event sink provider 参数，用于 CdpWarehouseSyntheticDataPathProbeService 流程中的校验、计算或对象转换。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 CdpWarehouseSyntheticDataPathProbeService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseSyntheticDataPathProbeService(
            CdpWarehouseSyntheticDataPathProbeRunMapper runMapper,
            ObjectProvider<CdpWarehouseEventSink> eventSinkProvider,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            ObjectMapper objectMapper) {
        this(runMapper, eventSinkProvider, dorisJdbcTemplate, null, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 初始化 CdpWarehouseSyntheticDataPathProbeService 实例。
     *
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventSinkProvider event sink provider 参数，用于 CdpWarehouseSyntheticDataPathProbeService 流程中的校验、计算或对象转换。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 CdpWarehouseSyntheticDataPathProbeService 流程中的校验、计算或对象转换。
     * @param sourceEventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public ProbeRunView run(Long tenantId, RunCommand command) {
        // 准备本次处理所需的上下文和中间变量。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<ProbeRunView> recent(Long tenantId, int limit) {
        return safeList(runMapper.listRecent(normalizeTenant(tenantId), boundLimit(limit)))
                .stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param doris doris 参数，用于 verifyOds 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param messageId 业务对象 ID，用于定位具体记录。
     * @param eventCode 业务编码，用于匹配对应类型或状态。
     * @param attempts attempts 参数，用于 verifyOds 流程中的校验、计算或对象转换。
     * @param delayMs delay ms 参数，用于 verifyOds 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 ods read sql 生成的文本或业务键。
     */
    private String odsReadSql() {
        return """
                SELECT COUNT(1)
                FROM canvas_ods.cdp_event_log
                WHERE tenant_id = ?
                  AND message_id = ?
                  AND event_code = ?
                """;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param messageId 业务对象 ID，用于定位具体记录。
     * @param eventCode 业务编码，用于匹配对应类型或状态。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param probeKey 业务键，用于在同一租户下定位资源。
     * @param time 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 syntheticEvent 流程生成的业务结果。
     */
    private CdpEventLogDO syntheticEvent(Long tenantId,
                                         String messageId,
                                         String eventCode,
                                         String userId,
                                         String probeKey,
                                         LocalDateTime time) {
        // 准备本次处理所需的上下文和中间变量。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return row;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param probeKey 业务键，用于在同一租户下定位资源。
     * @param messageId 业务对象 ID，用于定位具体记录。
     * @return 返回 properties 生成的文本或业务键。
     */
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

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param probeKey 业务键，用于在同一租户下定位资源。
     * @param sourceMode source mode 参数，用于 newRun 流程中的校验、计算或对象转换。
     * @param messageId 业务对象 ID，用于定位具体记录。
     * @param eventCode 业务编码，用于匹配对应类型或状态。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param strict strict 参数，用于 newRun 流程中的校验、计算或对象转换。
     * @param startedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 newRun 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param sourceStatus 业务状态，用于筛选或推进状态流转。
     * @param sinkStatus 业务状态，用于筛选或推进状态流转。
     * @param odsStatus 业务状态，用于筛选或推进状态流转。
     * @param odsRowCount ods row count 参数，用于 finish 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 finish 流程中的校验、计算或对象转换。
     * @param errorMessage error message 参数，用于 finish 流程中的校验、计算或对象转换。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evidence evidence 参数，用于 evidenceJson 流程中的校验、计算或对象转换。
     * @return 返回 evidence json 生成的文本或业务键。
     */
    private String evidenceJson(List<StepEvidence> evidence) {
        try {
            return objectMapper.writeValueAsString(evidence == null ? List.of() : evidence);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private ProbeRunView toView(CdpWarehouseSyntheticDataPathProbeRunDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evidence evidence 参数，用于 worstStatus 流程中的校验、计算或对象转换。
     * @return 返回 worst status 生成的文本或业务键。
     */
    private String worstStatus(List<StepEvidence> evidence) {
        if (evidence.stream().map(StepEvidence::status).anyMatch(STATUS_FAIL::equals)) {
            return STATUS_FAIL;
        }
        if (evidence.stream().map(StepEvidence::status).anyMatch(STATUS_WARN::equals)) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 event code 生成的文本或业务键。
     */
    private String eventCode(String value) {
        String eventCode = defaultString(value, DEFAULT_EVENT_CODE);
        if (!eventCode.startsWith("__warehouse_probe")) {
            throw new IllegalArgumentException("eventCode must use reserved __warehouse_probe prefix");
        }
        return eventCode;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 source mode 生成的文本或业务键。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedAttempts(Integer value) {
        int attempts = value == null || value <= 0 ? DEFAULT_VERIFY_ATTEMPTS : value;
        return Math.min(attempts, MAX_VERIFY_ATTEMPTS);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedDelayMs(Integer value) {
        int delay = value == null || value < 0 ? DEFAULT_VERIFY_DELAY_MS : value;
        return Math.min(delay, MAX_VERIFY_DELAY_MS);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundLimit(int value) {
        int limit = value <= 0 ? 20 : value;
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param delayMs delay ms 参数，用于 sleep 流程中的校验、计算或对象转换。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limit(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private List<CdpWarehouseSyntheticDataPathProbeRunDO> safeList(
            List<CdpWarehouseSyntheticDataPathProbeRunDO> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * RunCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RunCommand(
            String probeKey,
            String eventCode,
            Boolean strict,
            Integer verifyAttempts,
            Integer verifyDelayMs,
            String sourceMode) {
    }

    /**
     * ProbeRunView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * StepEvidence 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record StepEvidence(
            String key,
            String status,
            String reason) {
        public StepEvidence {
            status = status == null ? STATUS_FAIL : status.trim().toUpperCase(Locale.ROOT);
        }
    }
}
