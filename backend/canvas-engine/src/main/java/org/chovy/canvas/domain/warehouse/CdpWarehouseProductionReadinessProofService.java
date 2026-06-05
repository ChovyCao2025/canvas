package org.chovy.canvas.domain.warehouse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CdpWarehouseProductionReadinessProofService {

    private static final String MODE_HYBRID = "HYBRID";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";

    private final CdpWarehouseReadinessService readinessService;
    private final CdpWarehouseAvailabilityService availabilityService;
    private final CdpWarehouseConsumerAvailabilityService consumerAvailabilityService;
    private final ObjectProvider<CdpWarehousePrivacyErasureService> privacyErasureService;

    public CdpWarehouseProductionReadinessProofService(
            CdpWarehouseReadinessService readinessService,
            CdpWarehouseAvailabilityService availabilityService,
            CdpWarehouseConsumerAvailabilityService consumerAvailabilityService) {
        this(readinessService, availabilityService, consumerAvailabilityService, null);
    }

    @Autowired
    public CdpWarehouseProductionReadinessProofService(
            CdpWarehouseReadinessService readinessService,
            CdpWarehouseAvailabilityService availabilityService,
            CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
            ObjectProvider<CdpWarehousePrivacyErasureService> privacyErasureService) {
        this.readinessService = readinessService;
        this.availabilityService = availabilityService;
        this.consumerAvailabilityService = consumerAvailabilityService;
        this.privacyErasureService = privacyErasureService;
    }

    public ProductionReadinessProof proof(Long tenantId,
                                          LocalDateTime from,
                                          LocalDateTime to,
                                          String mode,
                                          List<String> contractKeys) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime windowEnd = to == null ? LocalDateTime.now() : to;
        LocalDateTime windowStart = from == null ? windowEnd.minusHours(1) : from;
        if (windowStart.isAfter(windowEnd)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
        String normalizedMode = normalizeMode(mode);
        List<ProofEvidence> evidence = new ArrayList<>();
        List<ConsumerContractProof> contracts = new ArrayList<>();
        CdpWarehouseReadinessService.ReadinessSummary readiness = readiness(
                scopedTenantId, evidence);
        CdpWarehouseAvailabilityService.AvailabilityDecision availability = availability(
                scopedTenantId, windowStart, windowEnd, normalizedMode, evidence);
        addContractEvidence(scopedTenantId, windowStart, windowEnd, contractKeys, evidence, contracts);
        CdpWarehousePrivacyErasureService.BacklogSummary privacyErasureBacklog =
                privacyErasureBacklog(scopedTenantId, evidence);
        return new ProductionReadinessProof(
                scopedTenantId,
                worstStatus(evidence.stream().map(ProofEvidence::status).toList()),
                LocalDateTime.now(),
                windowStart,
                windowEnd,
                normalizedMode,
                List.copyOf(evidence),
                readiness,
                availability,
                List.copyOf(contracts),
                privacyErasureBacklog);
    }

    private CdpWarehouseReadinessService.ReadinessSummary readiness(
            Long tenantId,
            List<ProofEvidence> evidence) {
        try {
            CdpWarehouseReadinessService.ReadinessSummary summary = readinessService.readiness(tenantId);
            String status = normalizeStatus(summary == null ? null : summary.status());
            evidence.add(new ProofEvidence("warehouse_readiness", status,
                    summary == null ? "warehouse readiness summary is missing" : statusReason(status)));
            return summary;
        } catch (RuntimeException e) {
            evidence.add(new ProofEvidence("warehouse_readiness", STATUS_FAIL,
                    "warehouse readiness failed: " + message(e)));
            return null;
        }
    }

    private CdpWarehouseAvailabilityService.AvailabilityDecision availability(
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to,
            String mode,
            List<ProofEvidence> evidence) {
        try {
            CdpWarehouseAvailabilityService.AvailabilityDecision decision =
                    availabilityService.evaluate(tenantId, from, to, mode);
            String status = normalizeStatus(decision == null ? null : decision.status());
            evidence.add(new ProofEvidence("window_availability", status,
                    decision == null ? "warehouse availability decision is missing" : statusReason(status)));
            return decision;
        } catch (RuntimeException e) {
            evidence.add(new ProofEvidence("window_availability", STATUS_FAIL,
                    "window availability failed: " + message(e)));
            return null;
        }
    }

    private void addContractEvidence(Long tenantId,
                                     LocalDateTime from,
                                     LocalDateTime to,
                                     List<String> contractKeys,
                                     List<ProofEvidence> evidence,
                                     List<ConsumerContractProof> contracts) {
        List<String> keys = safeContractKeys(contractKeys);
        if (keys.isEmpty()) {
            evidence.add(new ProofEvidence("consumer_contracts", STATUS_WARN,
                    "no consumer contracts requested"));
            return;
        }
        for (String contractKey : keys) {
            if (consumerAvailabilityService == null) {
                String reason = "consumer availability service is not configured";
                evidence.add(new ProofEvidence("consumer_contract:" + contractKey, STATUS_FAIL, reason));
                contracts.add(new ConsumerContractProof(contractKey, STATUS_FAIL, false, reason, null));
                continue;
            }
            try {
                CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation =
                        consumerAvailabilityService.evaluateContract(tenantId, contractKey, from, to);
                String rawStatus = normalizeStatus(evaluation == null ? null : evaluation.status());
                boolean allowed = evaluation != null && evaluation.allowed();
                String status = allowed ? rawStatus : STATUS_FAIL;
                String reason = evaluation == null
                        ? "consumer availability evaluation is missing"
                        : defaultReason(evaluation.message(), status, allowed);
                evidence.add(new ProofEvidence("consumer_contract:" + contractKey, status, reason));
                contracts.add(new ConsumerContractProof(contractKey, status, allowed, reason, evaluation));
            } catch (RuntimeException e) {
                String reason = "consumer contract evaluation failed: " + message(e);
                evidence.add(new ProofEvidence("consumer_contract:" + contractKey, STATUS_FAIL, reason));
                contracts.add(new ConsumerContractProof(contractKey, STATUS_FAIL, false, reason, null));
            }
        }
    }

    private CdpWarehousePrivacyErasureService.BacklogSummary privacyErasureBacklog(
            Long tenantId,
            List<ProofEvidence> evidence) {
        CdpWarehousePrivacyErasureService service =
                privacyErasureService == null ? null : privacyErasureService.getIfAvailable();
        if (service == null) {
            return null;
        }
        try {
            CdpWarehousePrivacyErasureService.BacklogSummary summary = service.summary(tenantId);
            String status = normalizeStatus(summary == null ? null : summary.status());
            evidence.add(new ProofEvidence("privacy_erasure_backlog", status,
                    summary == null ? "privacy erasure backlog summary is missing" : summary.reason()));
            return summary;
        } catch (RuntimeException e) {
            evidence.add(new ProofEvidence("privacy_erasure_backlog", STATUS_FAIL,
                    "privacy erasure backlog failed: " + message(e)));
            return null;
        }
    }

    private List<String> safeContractKeys(List<String> contractKeys) {
        if (contractKeys == null) {
            return List.of();
        }
        return contractKeys.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String defaultReason(String message, String status, boolean allowed) {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return allowed
                ? "consumer availability " + status + " allowed"
                : "consumer availability " + status + " blocked";
    }

    private String worstStatus(List<String> statuses) {
        if (statuses.stream().map(this::normalizeStatus).anyMatch(STATUS_FAIL::equals)) {
            return STATUS_FAIL;
        }
        if (statuses.stream().map(this::normalizeStatus).anyMatch(STATUS_WARN::equals)) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    private String normalizeStatus(String status) {
        String value = status == null ? STATUS_FAIL : status.trim().toUpperCase(Locale.ROOT);
        if (STATUS_PASS.equals(value) || STATUS_WARN.equals(value) || STATUS_FAIL.equals(value)) {
            return value;
        }
        return STATUS_FAIL;
    }

    private String normalizeMode(String mode) {
        return mode == null || mode.isBlank() ? MODE_HYBRID : mode.trim().toUpperCase(Locale.ROOT);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String statusReason(String status) {
        return "warehouse production evidence " + status.toLowerCase(Locale.ROOT);
    }

    private String message(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    public record ProductionReadinessProof(
            Long tenantId,
            String status,
            LocalDateTime generatedAt,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            String mode,
            List<ProofEvidence> evidence,
            CdpWarehouseReadinessService.ReadinessSummary readiness,
            CdpWarehouseAvailabilityService.AvailabilityDecision availability,
            List<ConsumerContractProof> contracts,
            CdpWarehousePrivacyErasureService.BacklogSummary privacyErasureBacklog) {
        public ProductionReadinessProof {
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
            contracts = contracts == null ? List.of() : List.copyOf(contracts);
        }
    }

    public record ProofEvidence(
            String key,
            String status,
            String reason) {
    }

    public record ConsumerContractProof(
            String contractKey,
            String status,
            boolean allowed,
            String reason,
            CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation) {
    }
}
