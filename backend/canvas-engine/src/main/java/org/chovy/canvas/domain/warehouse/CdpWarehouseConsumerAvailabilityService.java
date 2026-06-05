package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseAssetAvailabilityDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseConsumerAvailabilityContractDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseAssetAvailabilityMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseConsumerAvailabilityContractMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CdpWarehouseConsumerAvailabilityService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final String MODE_OFFLINE = "OFFLINE";
    private static final String MODE_REALTIME = "REALTIME";
    private static final String MODE_HYBRID = "HYBRID";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final String GATE_BLOCK_ON_FAIL = "BLOCK_ON_FAIL";
    private static final String GATE_BLOCK_ON_WARN = "BLOCK_ON_WARN";
    private static final TypeReference<List<AssetRef>> ASSET_REF_LIST = new TypeReference<>() {
    };

    private final CdpWarehouseAssetAvailabilityMapper assetAvailabilityMapper;
    private final CdpWarehouseConsumerAvailabilityContractMapper contractMapper;
    private final CdpWarehouseAvailabilityService availabilityService;
    private final ObjectMapper objectMapper;

    public CdpWarehouseConsumerAvailabilityService(
            CdpWarehouseAssetAvailabilityMapper assetAvailabilityMapper,
            CdpWarehouseConsumerAvailabilityContractMapper contractMapper,
            CdpWarehouseAvailabilityService availabilityService,
            ObjectMapper objectMapper) {
        this.assetAvailabilityMapper = assetAvailabilityMapper;
        this.contractMapper = contractMapper;
        this.availabilityService = availabilityService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public AssetAvailabilityView recordAssetAvailability(Long tenantId, AssetAvailabilityCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("asset availability command is required");
        }
        CdpWarehouseAssetAvailabilityDO row = new CdpWarehouseAssetAvailabilityDO();
        row.setTenantId(normalizeTenant(tenantId));
        row.setAssetType(normalizeAssetType(command.assetType()));
        row.setAssetKey(required(command.assetKey(), "assetKey"));
        row.setAvailabilityMode(normalizeMode(command.availabilityMode()));
        row.setWindowStart(command.windowStart());
        row.setWindowEnd(command.windowEnd());
        row.setAvailableUntil(requiredDate(command.availableUntil(), "availableUntil"));
        row.setStatus(normalizeAvailabilityStatus(command.status()));
        row.setEvidenceSource(upperDefault(command.evidenceSource(), "MANUAL"));
        row.setEvidenceRef(blankToNull(command.evidenceRef()));
        row.setReason(limitMessage(blankToNull(command.reason())));
        row.setObservedAt(command.observedAt() == null ? LocalDateTime.now() : command.observedAt());
        assetAvailabilityMapper.upsert(row);
        return toAssetView(row);
    }

    public List<AssetAvailabilityView> listAssetAvailability(Long tenantId,
                                                             String assetType,
                                                             String assetKey,
                                                             String mode,
                                                             Integer limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehouseAssetAvailabilityDO> query =
                new LambdaQueryWrapper<CdpWarehouseAssetAvailabilityDO>()
                        .in(CdpWarehouseAssetAvailabilityDO::getTenantId, tenantScope(scopedTenantId))
                        .orderByDesc(CdpWarehouseAssetAvailabilityDO::getObservedAt)
                        .orderByDesc(CdpWarehouseAssetAvailabilityDO::getId)
                        .last("LIMIT " + safeLimit(limit));
        if (hasText(assetType)) {
            query.eq(CdpWarehouseAssetAvailabilityDO::getAssetType, normalizeAssetType(assetType));
        }
        if (hasText(assetKey)) {
            query.eq(CdpWarehouseAssetAvailabilityDO::getAssetKey, assetKey.trim());
        }
        if (hasText(mode)) {
            query.eq(CdpWarehouseAssetAvailabilityDO::getAvailabilityMode, normalizeMode(mode));
        }
        return safeList(assetAvailabilityMapper.selectList(query)).stream()
                .map(this::toAssetView)
                .toList();
    }

    public ConsumerAvailabilityContractView upsertContract(Long tenantId,
                                                           ConsumerAvailabilityContractCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("consumer availability contract command is required");
        }
        List<AssetRef> assets = normalizeAssetRefs(command.requiredAssets());
        CdpWarehouseConsumerAvailabilityContractDO row = new CdpWarehouseConsumerAvailabilityContractDO();
        row.setTenantId(normalizeTenant(tenantId));
        row.setContractKey(required(command.contractKey(), "contractKey"));
        row.setConsumerType(upperRequired(command.consumerType(), "consumerType"));
        row.setConsumerRef(required(command.consumerRef(), "consumerRef"));
        row.setDatasetKey(blankToNull(command.datasetKey()));
        row.setMetricKey(blankToNull(command.metricKey()));
        row.setRequiredMode(normalizeMode(command.requiredMode()));
        row.setRequiredAssetsJson(json(assets));
        row.setGatePolicy(normalizeGatePolicy(command.gatePolicy()));
        row.setWarnToleranceMinutes(requireNonNegative(command.warnToleranceMinutes(), "warnToleranceMinutes"));
        row.setStatus(upperDefault(command.status(), STATUS_ACTIVE));
        row.setOwnerName(blankToNull(command.ownerName()));
        row.setDescription(limitMessage(blankToNull(command.description())));
        contractMapper.upsert(row);
        return toContractView(row);
    }

    public List<ConsumerAvailabilityContractView> listContracts(Long tenantId,
                                                                String consumerType,
                                                                String status) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehouseConsumerAvailabilityContractDO> query =
                new LambdaQueryWrapper<CdpWarehouseConsumerAvailabilityContractDO>()
                        .in(CdpWarehouseConsumerAvailabilityContractDO::getTenantId, tenantScope(scopedTenantId))
                        .orderByAsc(CdpWarehouseConsumerAvailabilityContractDO::getTenantId)
                        .orderByAsc(CdpWarehouseConsumerAvailabilityContractDO::getContractKey);
        if (hasText(consumerType)) {
            query.eq(CdpWarehouseConsumerAvailabilityContractDO::getConsumerType,
                    consumerType.trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(status)) {
            query.eq(CdpWarehouseConsumerAvailabilityContractDO::getStatus,
                    status.trim().toUpperCase(Locale.ROOT));
        }
        Map<String, ConsumerAvailabilityContractView> byKey = new LinkedHashMap<>();
        for (CdpWarehouseConsumerAvailabilityContractDO row : safeList(contractMapper.selectList(query))) {
            byKey.put(row.getContractKey(), toContractView(row));
        }
        return new ArrayList<>(byKey.values());
    }

    public ConsumerAvailabilityEvaluation evaluateContract(Long tenantId,
                                                           String contractKey,
                                                           LocalDateTime from,
                                                           LocalDateTime to) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseConsumerAvailabilityContractDO contract = findContract(scopedTenantId, contractKey);
        LocalDateTime requestedTo = to == null ? LocalDateTime.now() : to;
        LocalDateTime requestedFrom = from == null ? requestedTo.minusHours(1) : from;
        if (requestedFrom.isAfter(requestedTo)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
        CdpWarehouseAvailabilityService.AvailabilityDecision windowDecision =
                availabilityService.evaluate(scopedTenantId, requestedFrom, requestedTo, contract.getRequiredMode());
        List<AssetAvailabilityGate> assetGates = assetRefs(contract.getRequiredAssetsJson()).stream()
                .map(asset -> evaluateAsset(scopedTenantId, contract.getRequiredMode(),
                        requestedTo, safeInteger(contract.getWarnToleranceMinutes()), asset))
                .toList();
        String overallStatus = worstStatus(windowDecision.status(),
                assetGates.stream().map(AssetAvailabilityGate::status).toList());
        boolean allowed = allowed(overallStatus, contract.getGatePolicy());
        LocalDateTime evaluatedAt = LocalDateTime.now();
        String message = evaluationMessage(overallStatus, allowed, contract.getGatePolicy());
        contractMapper.updateEvaluation(
                contract.getTenantId(),
                contract.getContractKey(),
                evaluatedAt,
                overallStatus,
                message);
        return new ConsumerAvailabilityEvaluation(
                scopedTenantId,
                contract.getContractKey(),
                contract.getConsumerType(),
                contract.getConsumerRef(),
                contract.getRequiredMode(),
                requestedFrom,
                requestedTo,
                evaluatedAt,
                overallStatus,
                allowed,
                contract.getGatePolicy(),
                windowDecision,
                assetGates,
                message);
    }

    private AssetAvailabilityGate evaluateAsset(Long tenantId,
                                                String mode,
                                                LocalDateTime requestedTo,
                                                int warnToleranceMinutes,
                                                AssetRef asset) {
        CdpWarehouseAssetAvailabilityDO evidence = latestEvidence(tenantId, mode, asset);
        if (evidence == null) {
            return new AssetAvailabilityGate(
                    asset.assetType(),
                    asset.assetKey(),
                    STATUS_FAIL,
                    "asset availability evidence is missing",
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        if (STATUS_FAIL.equals(evidence.getStatus())) {
            return assetGate(evidence, STATUS_FAIL, defaultReason(evidence, "asset availability evidence failed"), 0L);
        }
        if (evidence.getAvailableUntil() == null) {
            return assetGate(evidence, STATUS_FAIL, "asset availability timestamp is missing", null);
        }
        long lagMinutes = positiveMinutesBetween(evidence.getAvailableUntil(), requestedTo);
        if (lagMinutes > 0) {
            String status = lagMinutes <= warnToleranceMinutes ? STATUS_WARN : STATUS_FAIL;
            return assetGate(evidence, status,
                    "requested window extends " + lagMinutes + "m past asset availability",
                    lagMinutes);
        }
        if (STATUS_WARN.equals(evidence.getStatus())) {
            return assetGate(evidence, STATUS_WARN, defaultReason(evidence, "asset availability evidence warned"), 0L);
        }
        return assetGate(evidence, STATUS_PASS, defaultReason(evidence, "asset availability covers requested window"), 0L);
    }

    private CdpWarehouseAssetAvailabilityDO latestEvidence(Long tenantId, String mode, AssetRef asset) {
        LambdaQueryWrapper<CdpWarehouseAssetAvailabilityDO> query =
                new LambdaQueryWrapper<CdpWarehouseAssetAvailabilityDO>()
                        .in(CdpWarehouseAssetAvailabilityDO::getTenantId, tenantScope(tenantId))
                        .eq(CdpWarehouseAssetAvailabilityDO::getAssetType, asset.assetType())
                        .eq(CdpWarehouseAssetAvailabilityDO::getAssetKey, asset.assetKey())
                        .in(CdpWarehouseAssetAvailabilityDO::getAvailabilityMode, modeCandidates(mode))
                        .orderByDesc(CdpWarehouseAssetAvailabilityDO::getObservedAt)
                        .orderByDesc(CdpWarehouseAssetAvailabilityDO::getId)
                        .last("LIMIT " + MAX_LIMIT);
        return safeList(assetAvailabilityMapper.selectList(query)).stream()
                .max(Comparator
                        .comparing((CdpWarehouseAssetAvailabilityDO row) -> tenantRank(row.getTenantId(), tenantId))
                        .thenComparing(row -> exactModeRank(row.getAvailabilityMode(), mode))
                        .thenComparing(CdpWarehouseAssetAvailabilityDO::getObservedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(CdpWarehouseAssetAvailabilityDO::getId,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    private AssetAvailabilityGate assetGate(CdpWarehouseAssetAvailabilityDO evidence,
                                            String status,
                                            String reason,
                                            Long lagMinutes) {
        return new AssetAvailabilityGate(
                evidence.getAssetType(),
                evidence.getAssetKey(),
                status,
                reason,
                evidence.getAvailableUntil(),
                lagMinutes,
                evidence.getEvidenceSource(),
                evidence.getEvidenceRef(),
                evidence.getObservedAt());
    }

    private CdpWarehouseConsumerAvailabilityContractDO findContract(Long tenantId, String contractKey) {
        String scopedContractKey = required(contractKey, "contractKey");
        LambdaQueryWrapper<CdpWarehouseConsumerAvailabilityContractDO> query =
                new LambdaQueryWrapper<CdpWarehouseConsumerAvailabilityContractDO>()
                        .in(CdpWarehouseConsumerAvailabilityContractDO::getTenantId, tenantScope(tenantId))
                        .eq(CdpWarehouseConsumerAvailabilityContractDO::getContractKey, scopedContractKey)
                        .orderByAsc(CdpWarehouseConsumerAvailabilityContractDO::getTenantId);
        CdpWarehouseConsumerAvailabilityContractDO selected = null;
        for (CdpWarehouseConsumerAvailabilityContractDO row : safeList(contractMapper.selectList(query))) {
            selected = row;
        }
        if (selected == null) {
            throw new IllegalArgumentException("consumer availability contract not found: " + scopedContractKey);
        }
        return selected;
    }

    private ConsumerAvailabilityContractView toContractView(CdpWarehouseConsumerAvailabilityContractDO row) {
        return new ConsumerAvailabilityContractView(
                row.getId(),
                row.getTenantId(),
                row.getContractKey(),
                row.getConsumerType(),
                row.getConsumerRef(),
                row.getDatasetKey(),
                row.getMetricKey(),
                row.getRequiredMode(),
                assetRefs(row.getRequiredAssetsJson()),
                row.getGatePolicy(),
                safeInteger(row.getWarnToleranceMinutes()),
                row.getStatus(),
                row.getOwnerName(),
                row.getDescription(),
                row.getLastEvaluatedAt(),
                row.getLastStatus(),
                row.getLastMessage(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private AssetAvailabilityView toAssetView(CdpWarehouseAssetAvailabilityDO row) {
        return new AssetAvailabilityView(
                row.getId(),
                row.getTenantId(),
                row.getAssetType(),
                row.getAssetKey(),
                row.getAvailabilityMode(),
                row.getWindowStart(),
                row.getWindowEnd(),
                row.getAvailableUntil(),
                row.getStatus(),
                row.getEvidenceSource(),
                row.getEvidenceRef(),
                row.getReason(),
                row.getObservedAt(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private List<AssetRef> normalizeAssetRefs(List<AssetRef> assets) {
        if (assets == null || assets.isEmpty()) {
            throw new IllegalArgumentException("requiredAssets is required");
        }
        Map<String, AssetRef> result = new LinkedHashMap<>();
        for (AssetRef asset : assets) {
            if (asset == null) {
                continue;
            }
            AssetRef normalized = new AssetRef(
                    normalizeAssetType(asset.assetType()),
                    required(asset.assetKey(), "assetKey"));
            result.put(normalized.assetType() + ":" + normalized.assetKey(), normalized);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("requiredAssets is required");
        }
        return List.copyOf(result.values());
    }

    private List<AssetRef> assetRefs(String value) {
        if (!hasText(value)) {
            return List.of();
        }
        try {
            List<AssetRef> assets = objectMapper.readValue(value, ASSET_REF_LIST);
            return normalizeAssetRefs(assets);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to read consumer availability assets", e);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize consumer availability contract", e);
        }
    }

    private String worstStatus(String windowStatus, List<String> assetStatuses) {
        List<String> statuses = new ArrayList<>();
        statuses.add(normalizeAvailabilityStatus(windowStatus));
        statuses.addAll(assetStatuses.stream().map(this::normalizeAvailabilityStatus).toList());
        if (statuses.contains(STATUS_FAIL)) {
            return STATUS_FAIL;
        }
        if (statuses.contains(STATUS_WARN)) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    private boolean allowed(String status, String gatePolicy) {
        if (STATUS_PASS.equals(status)) {
            return true;
        }
        return STATUS_WARN.equals(status) && GATE_BLOCK_ON_FAIL.equals(normalizeGatePolicy(gatePolicy));
    }

    private String evaluationMessage(String status, boolean allowed, String gatePolicy) {
        if (allowed) {
            return "consumer availability " + status + " allowed by " + normalizeGatePolicy(gatePolicy);
        }
        return "consumer availability " + status + " blocked by " + normalizeGatePolicy(gatePolicy);
    }

    private String defaultReason(CdpWarehouseAssetAvailabilityDO evidence, String fallback) {
        return hasText(evidence.getReason()) ? evidence.getReason() : fallback;
    }

    private long positiveMinutesBetween(LocalDateTime availableUntil, LocalDateTime requestedTo) {
        if (availableUntil == null || requestedTo == null || !requestedTo.isAfter(availableUntil)) {
            return 0L;
        }
        return Math.max(0L, Duration.between(availableUntil, requestedTo).toMinutes());
    }

    private List<String> modeCandidates(String mode) {
        String normalized = normalizeMode(mode);
        if (MODE_HYBRID.equals(normalized)) {
            return List.of(MODE_HYBRID);
        }
        return List.of(normalized, MODE_HYBRID);
    }

    private int tenantRank(Long candidateTenantId, Long tenantId) {
        return normalizeTenant(candidateTenantId).equals(normalizeTenant(tenantId)) ? 1 : 0;
    }

    private int exactModeRank(String candidateMode, String mode) {
        return normalizeMode(candidateMode).equals(normalizeMode(mode)) ? 1 : 0;
    }

    private List<Long> tenantScope(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (scopedTenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, scopedTenantId);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeMode(String mode) {
        String value = hasText(mode) ? mode.trim().toUpperCase(Locale.ROOT) : MODE_HYBRID;
        if (!MODE_OFFLINE.equals(value) && !MODE_REALTIME.equals(value) && !MODE_HYBRID.equals(value)) {
            throw new IllegalArgumentException("mode must be OFFLINE, REALTIME, or HYBRID");
        }
        return value;
    }

    private String normalizeAssetType(String assetType) {
        String value = upperRequired(assetType, "assetType");
        if (!"TABLE".equals(value) && !"DATASET".equals(value) && !"METRIC".equals(value)) {
            throw new IllegalArgumentException("assetType must be TABLE, DATASET, or METRIC");
        }
        return value;
    }

    private String normalizeAvailabilityStatus(String status) {
        String value = upperRequired(status, "status");
        if (!STATUS_PASS.equals(value) && !STATUS_WARN.equals(value) && !STATUS_FAIL.equals(value)) {
            throw new IllegalArgumentException("status must be PASS, WARN, or FAIL");
        }
        return value;
    }

    private String normalizeGatePolicy(String gatePolicy) {
        String value = upperDefault(gatePolicy, GATE_BLOCK_ON_WARN);
        if (!GATE_BLOCK_ON_FAIL.equals(value) && !GATE_BLOCK_ON_WARN.equals(value)) {
            throw new IllegalArgumentException("gatePolicy must be BLOCK_ON_FAIL or BLOCK_ON_WARN");
        }
        return value;
    }

    private Integer requireNonNegative(Integer value, String fieldName) {
        int normalized = value == null ? 0 : value;
        if (normalized < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return normalized;
    }

    private LocalDateTime requiredDate(LocalDateTime value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private String upperRequired(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
    }

    private String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String limitMessage(String value) {
        if (value == null || value.length() <= MAX_MESSAGE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_MESSAGE_LENGTH);
    }

    private int safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private int safeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    public record AssetRef(String assetType, String assetKey) {
    }

    public record AssetAvailabilityCommand(
            String assetType,
            String assetKey,
            String availabilityMode,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            LocalDateTime availableUntil,
            String status,
            String evidenceSource,
            String evidenceRef,
            String reason,
            LocalDateTime observedAt) {
    }

    public record AssetAvailabilityView(
            Long id,
            Long tenantId,
            String assetType,
            String assetKey,
            String availabilityMode,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            LocalDateTime availableUntil,
            String status,
            String evidenceSource,
            String evidenceRef,
            String reason,
            LocalDateTime observedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record ConsumerAvailabilityContractCommand(
            String contractKey,
            String consumerType,
            String consumerRef,
            String datasetKey,
            String metricKey,
            String requiredMode,
            List<AssetRef> requiredAssets,
            String gatePolicy,
            Integer warnToleranceMinutes,
            String status,
            String ownerName,
            String description) {
    }

    public record ConsumerAvailabilityContractView(
            Long id,
            Long tenantId,
            String contractKey,
            String consumerType,
            String consumerRef,
            String datasetKey,
            String metricKey,
            String requiredMode,
            List<AssetRef> requiredAssets,
            String gatePolicy,
            int warnToleranceMinutes,
            String status,
            String ownerName,
            String description,
            LocalDateTime lastEvaluatedAt,
            String lastStatus,
            String lastMessage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        public ConsumerAvailabilityContractView {
            requiredAssets = requiredAssets == null ? List.of() : List.copyOf(requiredAssets);
        }
    }

    public record ConsumerAvailabilityEvaluation(
            Long tenantId,
            String contractKey,
            String consumerType,
            String consumerRef,
            String mode,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo,
            LocalDateTime evaluatedAt,
            String status,
            boolean allowed,
            String gatePolicy,
            CdpWarehouseAvailabilityService.AvailabilityDecision windowDecision,
            List<AssetAvailabilityGate> assetGates,
            String message) {
        public ConsumerAvailabilityEvaluation {
            assetGates = assetGates == null ? List.of() : List.copyOf(assetGates);
        }
    }

    public record AssetAvailabilityGate(
            String assetType,
            String assetKey,
            String status,
            String reason,
            LocalDateTime availableUntil,
            Long lagMinutes,
            String evidenceSource,
            String evidenceRef,
            LocalDateTime observedAt) {
    }
}
