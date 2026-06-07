package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService {

    private static final int DEFAULT_SCAN_LIMIT = 50;
    private static final int DEFAULT_AUDIENCE_LIMIT = 100;
    private static final int MAX_LIST_LIMIT = 100;
    private static final int MAX_ERROR_LENGTH = 1024;

    private final CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService;
    private final CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper mapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService(
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper mapper,
            ObjectMapper objectMapper) {
        this(automationService, mapper, objectMapper, Clock.systemDefaultZone());
    }

    CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService(
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper mapper,
            ObjectMapper objectMapper,
            Clock clock) {
        this.automationService = automationService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public AutomationRunView runAndRecord(
            Long tenantId,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand command,
            String triggerSource) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand scopedCommand =
                command == null
                        ? new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand(
                        null, null, null, null)
                        : command;
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO row =
                runningRow(scopedTenantId, scopedCommand, triggerSource);
        mapper.insert(row);
        try {
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result =
                    automationService.run(scopedTenantId, scopedCommand);
            row.setStatus(status(result == null ? null : result.status()));
            row.setScanned(result == null ? 0 : result.scanned());
            row.setEligible(result == null ? 0 : result.eligible());
            row.setTriggered(result == null ? 0 : result.triggered());
            row.setSkipped(result == null ? 0 : result.skipped());
            row.setFailed(result == null ? 0 : result.failed());
            row.setResultJson(toJson(result));
            row.setFinishedAt(now());
            mapper.updateById(row);
            return toView(row, result);
        } catch (RuntimeException e) {
            row.setStatus("FAIL");
            row.setErrorMessage(truncate(e.getMessage()));
            row.setFinishedAt(now());
            mapper.updateById(row);
            throw e;
        }
    }

    public List<AutomationRunView> recent(Long tenantId, int limit) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        int scopedLimit = limit <= 0 ? 20 : Math.min(limit, MAX_LIST_LIMIT);
        List<CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO> rows = mapper.selectList(
                new LambdaQueryWrapper<CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO>()
                        .eq(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO::getTenantId, scopedTenantId)
                        .orderByDesc(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO::getStartedAt)
                        .last("LIMIT " + scopedLimit));
        return rows == null ? List.of() : rows.stream().map(row -> toView(row, null)).toList();
    }

    public AutomationRunView get(Long tenantId, Long id) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO row = id == null ? null : mapper.selectById(id);
        if (row == null || !scopedTenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("privacy audience rebuild automation run not found");
        }
        return toView(row, null);
    }

    private CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO runningRow(
            Long tenantId,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand command,
            String triggerSource) {
        LocalDateTime now = now();
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO row =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO();
        row.setTenantId(tenantId);
        row.setTriggerSource(source(triggerSource));
        row.setStatus("RUNNING");
        row.setActor(command.actor());
        row.setScanLimit(command.scanLimit() == null ? DEFAULT_SCAN_LIMIT : command.scanLimit());
        row.setAudienceLimit(command.audienceLimit() == null ? DEFAULT_AUDIENCE_LIMIT : command.audienceLimit());
        row.setRetryFailed(Boolean.TRUE.equals(command.retryFailed()) ? 1 : 0);
        row.setScanned(0);
        row.setEligible(0);
        row.setTriggered(0);
        row.setSkipped(0);
        row.setFailed(0);
        row.setStartedAt(now);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        return row;
    }

    private AutomationRunView toView(
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO row,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result) {
        return new AutomationRunView(
                row.getId(),
                row.getTenantId(),
                row.getTriggerSource(),
                row.getStatus(),
                row.getActor(),
                row.getScanLimit(),
                row.getAudienceLimit(),
                row.getRetryFailed() != null && row.getRetryFailed() == 1,
                value(row.getScanned()),
                value(row.getEligible()),
                value(row.getTriggered()),
                value(row.getSkipped()),
                value(row.getFailed()),
                row.getResultJson(),
                row.getErrorMessage(),
                row.getStartedAt(),
                row.getFinishedAt(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                result);
    }

    private String toJson(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result) {
        if (result == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize privacy audience rebuild automation result", e);
        }
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String status(String status) {
        return status == null || status.isBlank() ? "FAIL" : status.trim().toUpperCase(Locale.ROOT);
    }

    private String source(String triggerSource) {
        return triggerSource == null || triggerSource.isBlank()
                ? "MANUAL"
                : triggerSource.trim().toUpperCase(Locale.ROOT);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public record AutomationRunView(
            Long id,
            Long tenantId,
            String triggerSource,
            String status,
            String actor,
            Integer scanLimit,
            Integer audienceLimit,
            boolean retryFailed,
            int scanned,
            int eligible,
            int triggered,
            int skipped,
            int failed,
            String resultJson,
            String errorMessage,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result) {
    }
}
