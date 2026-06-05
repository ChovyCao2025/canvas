package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CdpWarehouseAvailabilityIncidentService {

    private static final String STATUS_PASS = "PASS";

    private final CdpWarehouseAvailabilityService availabilityService;
    private final CdpWarehouseIncidentService incidentService;

    public CdpWarehouseAvailabilityIncidentService(CdpWarehouseAvailabilityService availabilityService,
                                                   CdpWarehouseIncidentService incidentService) {
        this.availabilityService = availabilityService;
        this.incidentService = incidentService;
    }

    public ScanResult scan(Long tenantId,
                           LocalDateTime from,
                           LocalDateTime to,
                           String mode,
                           String operator) {
        CdpWarehouseAvailabilityService.AvailabilityDecision decision =
                availabilityService.evaluate(tenantId, from, to, mode);
        if (decision == null) {
            return new ScanResult(normalizeTenant(tenantId), normalizeMode(mode),
                    null, null, "UNKNOWN", 0, 0, 0, 0, 0);
        }
        List<CdpWarehouseAvailabilityService.AvailabilityGate> gates =
                decision.gates() == null ? List.of() : decision.gates();
        int opened = 0;
        int resolved = 0;
        int skipped = 0;
        int failed = 0;
        String normalizedOperator = normalizeOperator(operator);
        for (CdpWarehouseAvailabilityService.AvailabilityGate gate : gates) {
            if (gate == null) {
                skipped++;
                continue;
            }
            if (STATUS_PASS.equalsIgnoreCase(gate.status())) {
                try {
                    if (incidentService.resolveAvailabilityIncident(
                            decision.tenantId(), decision.mode(), gate.gateKey(), normalizedOperator)) {
                        resolved++;
                    } else {
                        skipped++;
                    }
                } catch (RuntimeException ignored) {
                    failed++;
                }
                continue;
            }
            try {
                incidentService.recordAvailabilityIncident(toIncidentInput(decision, gate));
                opened++;
            } catch (RuntimeException ignored) {
                failed++;
            }
        }
        return new ScanResult(
                decision.tenantId(),
                decision.mode(),
                decision.requestedFrom(),
                decision.requestedTo(),
                decision.status(),
                gates.size(),
                opened,
                resolved,
                skipped,
                failed);
    }

    private CdpWarehouseIncidentService.AvailabilityIncidentInput toIncidentInput(
            CdpWarehouseAvailabilityService.AvailabilityDecision decision,
            CdpWarehouseAvailabilityService.AvailabilityGate gate) {
        return new CdpWarehouseIncidentService.AvailabilityIncidentInput(
                decision.tenantId(),
                decision.mode(),
                decision.status(),
                gate.gateKey(),
                gate.status(),
                gate.reason(),
                decision.requestedFrom(),
                decision.requestedTo(),
                gate.availableUntil(),
                gate.lagMinutes(),
                gate.evidenceCount(),
                decision.generatedAt());
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeMode(String mode) {
        return mode == null || mode.isBlank() ? "HYBRID" : mode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOperator(String operator) {
        return operator == null || operator.isBlank() ? "warehouse-availability" : operator.trim();
    }

    public record ScanResult(
            Long tenantId,
            String mode,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo,
            String availabilityStatus,
            int totalGates,
            int opened,
            int resolved,
            int skipped,
            int failed) {
    }
}
