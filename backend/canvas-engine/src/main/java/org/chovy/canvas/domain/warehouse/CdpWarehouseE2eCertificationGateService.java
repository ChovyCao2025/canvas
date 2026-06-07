package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseE2eCertificationRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseE2eCertificationRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CdpWarehouseE2eCertificationGateService {

    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_FAIL = "FAIL";
    private static final int RECENT_LIMIT = 100;

    private final CdpWarehouseE2eCertificationRunMapper runMapper;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public CdpWarehouseE2eCertificationGateService(CdpWarehouseE2eCertificationRunMapper runMapper) {
        this(runMapper, Clock.systemDefaultZone());
    }

    CdpWarehouseE2eCertificationGateService(CdpWarehouseE2eCertificationRunMapper runMapper, Clock clock) {
        this.runMapper = runMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public GateDecision evaluate(Long tenantId,
                                 String mode,
                                 List<String> contractKeys,
                                 boolean requirePhysical,
                                 long maxAgeMinutes) {
        return evaluate(tenantId, mode, contractKeys, requirePhysical, false, maxAgeMinutes);
    }

    public GateDecision evaluate(Long tenantId,
                                 String mode,
                                 List<String> contractKeys,
                                 boolean requirePhysical,
                                 boolean requireRealtime,
                                 long maxAgeMinutes) {
        return evaluate(tenantId, mode, contractKeys, requirePhysical, requireRealtime, false, maxAgeMinutes);
    }

    public GateDecision evaluate(Long tenantId,
                                 String mode,
                                 List<String> contractKeys,
                                 boolean requirePhysical,
                                 boolean requireRealtime,
                                 boolean requireDataPathProof,
                                 long maxAgeMinutes) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String normalizedMode = normalizeMode(mode);
        List<String> requiredContracts = safeContractKeys(contractKeys);
        long safeMaxAge = Math.max(1, maxAgeMinutes);
        CdpWarehouseE2eCertificationRunDO matched =
                latestMatchingRun(scopedTenantId, normalizedMode, requirePhysical, requireRealtime, requireDataPathProof);
        if (matched == null) {
            return decision(scopedTenantId, STATUS_FAIL, "no matching certification run",
                    null, null, null, null, normalizedMode, requirePhysical, requireRealtime,
                    requireDataPathProof, safeMaxAge, requiredContracts);
        }

        LocalDateTime finishedAt = matched.getFinishedAt();
        LocalDateTime expiresAt = finishedAt == null ? null : finishedAt.plusMinutes(safeMaxAge);
        List<String> runContracts = safeContractKeys(parseContractKeys(matched.getContractKeysJson()));
        List<String> missingContracts = requiredContracts.stream()
                .filter(required -> !runContracts.contains(required))
                .toList();
        if (!missingContracts.isEmpty()) {
            return decision(scopedTenantId, STATUS_FAIL,
                    "missing contract keys: " + String.join(",", missingContracts),
                    matched.getId(), matched.getStatus(), finishedAt, expiresAt,
                    normalizedMode, requirePhysical, requireRealtime, requireDataPathProof,
                    safeMaxAge, requiredContracts);
        }
        if (!STATUS_PASS.equals(normalizeStatus(matched.getStatus()))) {
            return decision(scopedTenantId, STATUS_FAIL,
                    "latest matching run status is " + normalizeStatus(matched.getStatus()),
                    matched.getId(), matched.getStatus(), finishedAt, expiresAt,
                    normalizedMode, requirePhysical, requireRealtime, requireDataPathProof,
                    safeMaxAge, requiredContracts);
        }
        if (finishedAt == null) {
            return decision(scopedTenantId, STATUS_FAIL,
                    "latest matching run has no finishedAt",
                    matched.getId(), matched.getStatus(), null, null,
                    normalizedMode, requirePhysical, requireRealtime, requireDataPathProof,
                    safeMaxAge, requiredContracts);
        }
        if (expiresAt.isBefore(now())) {
            return decision(scopedTenantId, STATUS_FAIL,
                    "certification evidence is stale",
                    matched.getId(), matched.getStatus(), finishedAt, expiresAt,
                    normalizedMode, requirePhysical, requireRealtime, requireDataPathProof,
                    safeMaxAge, requiredContracts);
        }
        if (requireRealtime && requireDataPathProof) {
            String dataPathReason = realtimeDataPathSourceFailureReason(matched.getDataPathProofJson());
            if (dataPathReason != null) {
                return decision(scopedTenantId, STATUS_FAIL,
                        dataPathReason,
                        matched.getId(), matched.getStatus(), finishedAt, expiresAt,
                        normalizedMode, requirePhysical, requireRealtime, requireDataPathProof,
                        safeMaxAge, requiredContracts);
            }
        }
        return decision(scopedTenantId, STATUS_PASS,
                "fresh PASS certification evidence",
                matched.getId(), matched.getStatus(), finishedAt, expiresAt,
                normalizedMode, requirePhysical, requireRealtime, requireDataPathProof, safeMaxAge, requiredContracts);
    }

    private CdpWarehouseE2eCertificationRunDO latestMatchingRun(Long tenantId,
                                                                String mode,
                                                                boolean requirePhysical,
                                                                boolean requireRealtime,
                                                                boolean requireDataPathProof) {
        List<CdpWarehouseE2eCertificationRunDO> rows = runMapper.selectList(
                new LambdaQueryWrapper<CdpWarehouseE2eCertificationRunDO>()
                        .eq(CdpWarehouseE2eCertificationRunDO::getTenantId, tenantId)
                        .orderByDesc(CdpWarehouseE2eCertificationRunDO::getId)
                        .last("LIMIT " + RECENT_LIMIT));
        if (rows == null) {
            return null;
        }
        return rows.stream()
                .filter(row -> mode.equals(normalizeMode(row.getMode())))
                .filter(row -> !requirePhysical || Integer.valueOf(1).equals(row.getRequirePhysical()))
                .filter(row -> !requireRealtime || Integer.valueOf(1).equals(row.getRequireRealtime()))
                .filter(row -> !requireDataPathProof || Integer.valueOf(1).equals(row.getRequireDataPathProof()))
                .findFirst()
                .orElse(null);
    }

    private List<String> parseContractKeys(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String realtimeDataPathSourceFailureReason(String dataPathProofJson) {
        if (dataPathProofJson == null || dataPathProofJson.isBlank()) {
            return "realtime certification requires dataPathProof sourceMode MYSQL_CDC but proof is missing";
        }
        try {
            JsonNode proof = objectMapper.readTree(dataPathProofJson);
            String sourceMode = text(proof, "sourceMode");
            String sourceStatus = text(proof, "sourceStatus");
            String odsStatus = text(proof, "odsStatus");
            String status = text(proof, "status");
            if (!"MYSQL_CDC".equals(normalizeStatusLike(sourceMode))) {
                return "realtime certification requires dataPathProof sourceMode MYSQL_CDC but was "
                        + defaultText(sourceMode, "missing");
            }
            if (!STATUS_PASS.equals(normalizeStatus(sourceStatus))) {
                return "realtime certification requires dataPathProof sourceStatus PASS but was "
                        + defaultText(sourceStatus, "missing");
            }
            if (!STATUS_PASS.equals(normalizeStatus(odsStatus))) {
                return "realtime certification requires dataPathProof odsStatus PASS but was "
                        + defaultText(odsStatus, "missing");
            }
            if (status != null && !STATUS_PASS.equals(normalizeStatus(status))) {
                return "realtime certification requires dataPathProof status PASS but was " + status;
            }
            return null;
        } catch (Exception e) {
            return "realtime certification requires dataPathProof sourceMode MYSQL_CDC but proof JSON is invalid";
        }
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        return node.get(fieldName).asText();
    }

    private String normalizeStatusLike(String value) {
        return value == null || value.isBlank()
                ? ""
                : value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private GateDecision decision(Long tenantId,
                                  String status,
                                  String reason,
                                  Long matchedRunId,
                                  String matchedRunStatus,
                                  LocalDateTime matchedFinishedAt,
                                  LocalDateTime expiresAt,
                                  String mode,
                                  boolean requirePhysical,
                                  boolean requireRealtime,
                                  boolean requireDataPathProof,
                                  long maxAgeMinutes,
                                  List<String> contractKeys) {
        return new GateDecision(
                tenantId,
                status,
                reason,
                matchedRunId,
                matchedRunStatus,
                matchedFinishedAt,
                expiresAt,
                mode,
                requirePhysical,
                requireRealtime,
                requireDataPathProof,
                maxAgeMinutes,
                contractKeys == null ? List.of() : List.copyOf(contractKeys));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
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

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeMode(String mode) {
        return mode == null || mode.isBlank() ? "HYBRID" : mode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? STATUS_FAIL : status.trim().toUpperCase(Locale.ROOT);
    }

    public record GateDecision(Long tenantId,
                               String status,
                               String reason,
                               Long matchedRunId,
                               String matchedRunStatus,
                               LocalDateTime matchedFinishedAt,
                               LocalDateTime expiresAt,
                               String mode,
                               boolean requirePhysical,
                               boolean requireRealtime,
                               boolean requireDataPathProof,
                               long maxAgeMinutes,
                               List<String> contractKeys) {
        public GateDecision {
            contractKeys = contractKeys == null ? List.of() : List.copyOf(contractKeys);
        }
    }
}
