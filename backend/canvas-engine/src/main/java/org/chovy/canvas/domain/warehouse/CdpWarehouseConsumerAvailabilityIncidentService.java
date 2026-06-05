package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CdpWarehouseConsumerAvailabilityIncidentService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PASS = "PASS";

    private final CdpWarehouseConsumerAvailabilityService consumerAvailabilityService;
    private final CdpWarehouseIncidentService incidentService;

    public CdpWarehouseConsumerAvailabilityIncidentService(
            CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
            CdpWarehouseIncidentService incidentService) {
        this.consumerAvailabilityService = consumerAvailabilityService;
        this.incidentService = incidentService;
    }

    public ScanResult scan(Long tenantId,
                           String contractKey,
                           String consumerType,
                           LocalDateTime from,
                           LocalDateTime to,
                           String operator) {
        Long scopedTenantId = normalizeTenant(tenantId);
        List<String> contractKeys = contractKeys(scopedTenantId, contractKey, consumerType);
        int opened = 0;
        int resolved = 0;
        int skipped = 0;
        int failed = 0;
        String normalizedOperator = normalizeOperator(operator);
        LocalDateTime startedAt = LocalDateTime.now();
        String worstStatus = STATUS_PASS;
        for (String key : contractKeys) {
            if (!hasText(key)) {
                skipped++;
                continue;
            }
            try {
                CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation =
                        consumerAvailabilityService.evaluateContract(scopedTenantId, key, from, to);
                if (evaluation == null) {
                    failed++;
                    worstStatus = worstStatus(worstStatus, "FAIL");
                    continue;
                }
                worstStatus = worstStatus(worstStatus, evaluation.status());
                if (STATUS_PASS.equalsIgnoreCase(evaluation.status())) {
                    if (incidentService.resolveConsumerAvailabilityIncident(
                            evaluation.tenantId(), evaluation.contractKey(), normalizedOperator)) {
                        resolved++;
                    } else {
                        skipped++;
                    }
                    continue;
                }
                incidentService.recordConsumerAvailabilityIncident(toIncidentInput(evaluation));
                opened++;
            } catch (RuntimeException ignored) {
                failed++;
                worstStatus = worstStatus(worstStatus, "FAIL");
            }
        }
        return new ScanResult(
                scopedTenantId,
                blankToNull(contractKey),
                normalizeConsumerType(consumerType),
                from,
                to,
                worstStatus,
                contractKeys.size(),
                opened,
                resolved,
                skipped,
                failed,
                startedAt,
                LocalDateTime.now());
    }

    private List<String> contractKeys(Long tenantId, String contractKey, String consumerType) {
        if (hasText(contractKey)) {
            return List.of(contractKey.trim());
        }
        return safeList(consumerAvailabilityService.listContracts(
                        tenantId,
                        normalizeConsumerType(consumerType),
                        STATUS_ACTIVE))
                .stream()
                .map(CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractView::contractKey)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private CdpWarehouseIncidentService.ConsumerAvailabilityIncidentInput toIncidentInput(
            CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation) {
        return new CdpWarehouseIncidentService.ConsumerAvailabilityIncidentInput(
                evaluation.tenantId(),
                evaluation.contractKey(),
                evaluation.consumerType(),
                evaluation.consumerRef(),
                evaluation.mode(),
                evaluation.status(),
                evaluation.allowed(),
                evaluation.gatePolicy(),
                evaluation.message(),
                evaluation.requestedFrom(),
                evaluation.requestedTo(),
                evaluation.evaluatedAt(),
                assetGateSummaries(evaluation.assetGates()));
    }

    private List<String> assetGateSummaries(
            List<CdpWarehouseConsumerAvailabilityService.AssetAvailabilityGate> assetGates) {
        return safeList(assetGates).stream()
                .map(gate -> gate.assetType()
                        + ":" + gate.assetKey()
                        + "=" + gate.status()
                        + "(" + gate.reason() + ")")
                .toList();
    }

    private String worstStatus(String current, String next) {
        String left = normalizeStatus(current);
        String right = normalizeStatus(next);
        if ("FAIL".equals(left) || "FAIL".equals(right)) {
            return "FAIL";
        }
        if ("WARN".equals(left) || "WARN".equals(right)) {
            return "WARN";
        }
        return STATUS_PASS;
    }

    private String normalizeStatus(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : STATUS_PASS;
    }

    private String normalizeConsumerType(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeOperator(String operator) {
        return hasText(operator) ? operator.trim() : "consumer-availability-incident";
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record ScanResult(
            Long tenantId,
            String contractKey,
            String consumerType,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo,
            String worstStatus,
            int totalContracts,
            int opened,
            int resolved,
            int skipped,
            int failed,
            LocalDateTime startedAt,
            LocalDateTime finishedAt) {
    }
}
