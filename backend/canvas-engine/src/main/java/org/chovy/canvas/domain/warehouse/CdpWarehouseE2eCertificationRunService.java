package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseE2eCertificationRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseE2eCertificationRunMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CdpWarehouseE2eCertificationRunService {

    private static final String STATUS_FAIL = "FAIL";

    private final CdpWarehousePhysicalE2eCertificationService certificationService;
    private final CdpWarehouseE2eCertificationRunMapper runMapper;
    private final ObjectMapper objectMapper;

    public CdpWarehouseE2eCertificationRunService(
            CdpWarehousePhysicalE2eCertificationService certificationService,
            CdpWarehouseE2eCertificationRunMapper runMapper) {
        this.certificationService = certificationService;
        this.runMapper = runMapper;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public CertificationRunView run(Long tenantId,
                                    LocalDateTime from,
                                    LocalDateTime to,
                                    String mode,
                                    List<String> contractKeys,
                                    boolean requirePhysical,
                                    String requestedBy) {
        return run(tenantId, from, to, mode, contractKeys, requirePhysical, false, requestedBy);
    }

    public CertificationRunView run(Long tenantId,
                                    LocalDateTime from,
                                    LocalDateTime to,
                                    String mode,
                                    List<String> contractKeys,
                                    boolean requirePhysical,
                                    boolean requireRealtime,
                                    String requestedBy) {
        return run(tenantId, from, to, mode, contractKeys, requirePhysical, requireRealtime, false, requestedBy);
    }

    public CertificationRunView run(Long tenantId,
                                    LocalDateTime from,
                                    LocalDateTime to,
                                    String mode,
                                    List<String> contractKeys,
                                    boolean requirePhysical,
                                    boolean requireRealtime,
                                    boolean requireDataPathProof,
                                    String requestedBy) {
        LocalDateTime startedAt = LocalDateTime.now();
        CdpWarehouseE2eCertificationRunDO row = new CdpWarehouseE2eCertificationRunDO();
        row.setTenantId(normalizeTenant(tenantId));
        row.setMode(normalizeMode(mode));
        row.setRequirePhysical(requirePhysical ? 1 : 0);
        row.setRequireRealtime(requireRealtime ? 1 : 0);
        row.setRequireDataPathProof(requireDataPathProof ? 1 : 0);
        row.setWindowStart(from);
        row.setWindowEnd(to);
        row.setContractKeysJson(toJson(safeContractKeys(contractKeys)));
        row.setRequestedBy(requestedBy);
        row.setStartedAt(startedAt);
        try {
            CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                    certificationService.certify(row.getTenantId(), from, to, row.getMode(),
                            safeContractKeys(contractKeys), requirePhysical, requireRealtime, requireDataPathProof);
            row.setStatus(certification.status());
            row.setWindowStart(certification.windowStart());
            row.setWindowEnd(certification.windowEnd());
            row.setEvidenceJson(toJson(certification.evidence()));
            row.setProductionReadinessJson(toJson(certification.productionReadiness()));
            row.setLiveTableInspectionJson(toJson(certification.liveTableInspection()));
            row.setRealtimePipelineStatusJson(toJson(certification.realtimePipelineStatus()));
            row.setRealtimeJobStatusJson(toJson(certification.realtimeJobStatus()));
            row.setDataPathProofJson(toJson(certification.dataPathProof()));
        } catch (RuntimeException ex) {
            row.setStatus(STATUS_FAIL);
            row.setErrorMessage(message(ex));
            row.setEvidenceJson("[]");
        }
        row.setFinishedAt(LocalDateTime.now());
        runMapper.insert(row);
        return toView(row);
    }

    public List<CertificationRunView> recent(Long tenantId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return runMapper.selectList(new LambdaQueryWrapper<CdpWarehouseE2eCertificationRunDO>()
                        .eq(CdpWarehouseE2eCertificationRunDO::getTenantId, normalizeTenant(tenantId))
                        .orderByDesc(CdpWarehouseE2eCertificationRunDO::getId)
                        .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toView)
                .toList();
    }

    public CertificationRunView get(Long tenantId, Long id) {
        CdpWarehouseE2eCertificationRunDO row = runMapper.selectById(id);
        if (row == null || !normalizeTenant(tenantId).equals(row.getTenantId())) {
            throw new IllegalArgumentException("certification run not found: " + id);
        }
        return toView(row);
    }

    private CertificationRunView toView(CdpWarehouseE2eCertificationRunDO row) {
        return new CertificationRunView(
                row.getId(),
                row.getTenantId(),
                row.getStatus(),
                row.getMode(),
                Integer.valueOf(1).equals(row.getRequirePhysical()),
                Integer.valueOf(1).equals(row.getRequireRealtime()),
                Integer.valueOf(1).equals(row.getRequireDataPathProof()),
                row.getWindowStart(),
                row.getWindowEnd(),
                row.getContractKeysJson(),
                row.getEvidenceJson(),
                row.getProductionReadinessJson(),
                row.getLiveTableInspectionJson(),
                row.getRealtimePipelineStatusJson(),
                row.getRealtimeJobStatusJson(),
                row.getDataPathProofJson(),
                row.getRequestedBy(),
                row.getStartedAt(),
                row.getFinishedAt(),
                row.getErrorMessage());
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize certification run evidence", ex);
        }
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeMode(String mode) {
        return mode == null || mode.isBlank() ? "HYBRID" : mode.trim().toUpperCase(Locale.ROOT);
    }

    private String message(RuntimeException ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    public record CertificationRunView(Long id,
                                       Long tenantId,
                                       String status,
                                       String mode,
                                       boolean requirePhysical,
                                       boolean requireRealtime,
                                       boolean requireDataPathProof,
                                       LocalDateTime windowStart,
                                       LocalDateTime windowEnd,
                                       String contractKeysJson,
                                       String evidenceJson,
                                       String productionReadinessJson,
                                       String liveTableInspectionJson,
                                       String realtimePipelineStatusJson,
                                       String realtimeJobStatusJson,
                                       String dataPathProofJson,
                                       String requestedBy,
                                       LocalDateTime startedAt,
                                       LocalDateTime finishedAt,
                                       String errorMessage) {
    }
}
