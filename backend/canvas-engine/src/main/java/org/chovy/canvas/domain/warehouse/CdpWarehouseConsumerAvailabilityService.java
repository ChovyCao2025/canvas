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
/**
 * CdpWarehouseConsumerAvailabilityService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 CdpWarehouseConsumerAvailabilityService 实例。
     *
     * @param assetAvailabilityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param assetType 类型标识，用于选择对应处理分支。
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @param mode mode 参数，用于 listAssetAvailability 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
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
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(assetType)) {
            query.eq(CdpWarehouseAssetAvailabilityDO::getAssetType, normalizeAssetType(assetType));
        }
        if (hasText(assetKey)) {
            query.eq(CdpWarehouseAssetAvailabilityDO::getAssetKey, assetKey.trim());
        }
        if (hasText(mode)) {
            query.eq(CdpWarehouseAssetAvailabilityDO::getAvailabilityMode, normalizeMode(mode));
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return safeList(assetAvailabilityMapper.selectList(query)).stream()
                .map(this::toAssetView)
                .toList();
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public ConsumerAvailabilityContractView upsertContract(Long tenantId,
                                                           ConsumerAvailabilityContractCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        contractMapper.upsert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toContractView(row);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param consumerType 类型标识，用于选择对应处理分支。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<ConsumerAvailabilityContractView> listContracts(Long tenantId,
                                                                String consumerType,
                                                                String status) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehouseConsumerAvailabilityContractDO> query =
                new LambdaQueryWrapper<CdpWarehouseConsumerAvailabilityContractDO>()
                        .in(CdpWarehouseConsumerAvailabilityContractDO::getTenantId, tenantScope(scopedTenantId))
                        .orderByAsc(CdpWarehouseConsumerAvailabilityContractDO::getTenantId)
                        .orderByAsc(CdpWarehouseConsumerAvailabilityContractDO::getContractKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(consumerType)) {
            query.eq(CdpWarehouseConsumerAvailabilityContractDO::getConsumerType,
                    consumerType.trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(status)) {
            query.eq(CdpWarehouseConsumerAvailabilityContractDO::getStatus,
                    status.trim().toUpperCase(Locale.ROOT));
        }
        Map<String, ConsumerAvailabilityContractView> byKey = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseConsumerAvailabilityContractDO row : safeList(contractMapper.selectList(query))) {
            byKey.put(row.getContractKey(), toContractView(row));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ArrayList<>(byKey.values());
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contractKey 业务键，用于在同一租户下定位资源。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @return 返回 evaluateContract 流程生成的业务结果。
     */
    public ConsumerAvailabilityEvaluation evaluateContract(Long tenantId,
                                                           String contractKey,
                                                           LocalDateTime from,
                                                           LocalDateTime to) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseConsumerAvailabilityContractDO contract = findContract(scopedTenantId, contractKey);
        LocalDateTime requestedTo = to == null ? LocalDateTime.now() : to;
        LocalDateTime requestedFrom = from == null ? requestedTo.minusHours(1) : from;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (requestedFrom.isAfter(requestedTo)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
        CdpWarehouseAvailabilityService.AvailabilityDecision windowDecision =
                availabilityService.evaluate(scopedTenantId, requestedFrom, requestedTo, contract.getRequiredMode());
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<AssetAvailabilityGate> assetGates = assetRefs(contract.getRequiredAssetsJson()).stream()
                .map(asset -> evaluateAsset(scopedTenantId, contract.getRequiredMode(),
                        requestedTo, safeInteger(contract.getWarnToleranceMinutes()), asset))
                .toList();
        String overallStatus = worstStatus(windowDecision.status(),
                assetGates.stream().map(AssetAvailabilityGate::status).toList());
        boolean allowed = allowed(overallStatus, contract.getGatePolicy());
        LocalDateTime evaluatedAt = LocalDateTime.now();
        String message = evaluationMessage(overallStatus, allowed, contract.getGatePolicy());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param mode mode 参数，用于 evaluateAsset 流程中的校验、计算或对象转换。
     * @param requestedTo 时间或范围边界，用于限定统计窗口。
     * @param warnToleranceMinutes warn tolerance minutes 参数，用于 evaluateAsset 流程中的校验、计算或对象转换。
     * @param asset asset 参数，用于 evaluateAsset 流程中的校验、计算或对象转换。
     * @return 返回 evaluateAsset 流程生成的业务结果。
     */
    private AssetAvailabilityGate evaluateAsset(Long tenantId,
                                                String mode,
                                                LocalDateTime requestedTo,
                                                int warnToleranceMinutes,
                                                AssetRef asset) {
        // 准备本次处理所需的上下文和中间变量。
        CdpWarehouseAssetAvailabilityDO evidence = latestEvidence(tenantId, mode, asset);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return assetGate(evidence, STATUS_PASS, defaultReason(evidence, "asset availability covers requested window"), 0L);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param mode mode 参数，用于 latestEvidence 流程中的校验、计算或对象转换。
     * @param asset asset 参数，用于 latestEvidence 流程中的校验、计算或对象转换。
     * @return 返回 latestEvidence 流程生成的业务结果。
     */
    private CdpWarehouseAssetAvailabilityDO latestEvidence(Long tenantId, String mode, AssetRef asset) {
        // 准备本次处理所需的上下文和中间变量。
        LambdaQueryWrapper<CdpWarehouseAssetAvailabilityDO> query =
                new LambdaQueryWrapper<CdpWarehouseAssetAvailabilityDO>()
                        .in(CdpWarehouseAssetAvailabilityDO::getTenantId, tenantScope(tenantId))
                        .eq(CdpWarehouseAssetAvailabilityDO::getAssetType, asset.assetType())
                        .eq(CdpWarehouseAssetAvailabilityDO::getAssetKey, asset.assetKey())
                        .in(CdpWarehouseAssetAvailabilityDO::getAvailabilityMode, modeCandidates(mode))
                        .orderByDesc(CdpWarehouseAssetAvailabilityDO::getObservedAt)
                        .orderByDesc(CdpWarehouseAssetAvailabilityDO::getId)
                        .last("LIMIT " + MAX_LIMIT);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evidence evidence 参数，用于 assetGate 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param lagMinutes lag minutes 参数，用于 assetGate 流程中的校验、计算或对象转换。
     * @return 返回 assetGate 流程生成的业务结果。
     */
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contractKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private CdpWarehouseConsumerAvailabilityContractDO findContract(Long tenantId, String contractKey) {
        String scopedContractKey = required(contractKey, "contractKey");
        LambdaQueryWrapper<CdpWarehouseConsumerAvailabilityContractDO> query =
                new LambdaQueryWrapper<CdpWarehouseConsumerAvailabilityContractDO>()
                        .in(CdpWarehouseConsumerAvailabilityContractDO::getTenantId, tenantScope(tenantId))
                        .eq(CdpWarehouseConsumerAvailabilityContractDO::getContractKey, scopedContractKey)
                        .orderByAsc(CdpWarehouseConsumerAvailabilityContractDO::getTenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CdpWarehouseConsumerAvailabilityContractDO selected = null;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseConsumerAvailabilityContractDO row : safeList(contractMapper.selectList(query))) {
            selected = row;
        }
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (selected == null) {
            throw new IllegalArgumentException("consumer availability contract not found: " + scopedContractKey);
        }
        return selected;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private ConsumerAvailabilityContractView toContractView(CdpWarehouseConsumerAvailabilityContractDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param assets assets 参数，用于 normalizeAssetRefs 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<AssetRef> normalizeAssetRefs(List<AssetRef> assets) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (assets == null || assets.isEmpty()) {
            throw new IllegalArgumentException("requiredAssets is required");
        }
        Map<String, AssetRef> result = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(result.values());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 asset refs 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize consumer availability contract", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param windowStatus 业务状态，用于筛选或推进状态流转。
     * @param assetStatuses asset statuses 参数，用于 worstStatus 流程中的校验、计算或对象转换。
     * @return 返回 worst status 生成的文本或业务键。
     */
    private String worstStatus(String windowStatus, List<String> assetStatuses) {
        List<String> statuses = new ArrayList<>();
        statuses.add(normalizeAvailabilityStatus(windowStatus));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        statuses.addAll(assetStatuses.stream().map(this::normalizeAvailabilityStatus).toList());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (statuses.contains(STATUS_FAIL)) {
            return STATUS_FAIL;
        }
        if (statuses.contains(STATUS_WARN)) {
            return STATUS_WARN;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return STATUS_PASS;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param gatePolicy gate policy 参数，用于 allowed 流程中的校验、计算或对象转换。
     * @return 返回 allowed 的布尔判断结果。
     */
    private boolean allowed(String status, String gatePolicy) {
        if (STATUS_PASS.equals(status)) {
            return true;
        }
        return STATUS_WARN.equals(status) && GATE_BLOCK_ON_FAIL.equals(normalizeGatePolicy(gatePolicy));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param allowed allowed 参数，用于 evaluationMessage 流程中的校验、计算或对象转换。
     * @param gatePolicy gate policy 参数，用于 evaluationMessage 流程中的校验、计算或对象转换。
     * @return 返回 evaluation message 生成的文本或业务键。
     */
    private String evaluationMessage(String status, boolean allowed, String gatePolicy) {
        if (allowed) {
            return "consumer availability " + status + " allowed by " + normalizeGatePolicy(gatePolicy);
        }
        return "consumer availability " + status + " blocked by " + normalizeGatePolicy(gatePolicy);
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param evidence evidence 参数，用于 defaultReason 流程中的校验、计算或对象转换。
     * @param fallback fallback 参数，用于 defaultReason 流程中的校验、计算或对象转换。
     * @return 返回 default reason 生成的文本或业务键。
     */
    private String defaultReason(CdpWarehouseAssetAvailabilityDO evidence, String fallback) {
        return hasText(evidence.getReason()) ? evidence.getReason() : fallback;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param availableUntil available until 参数，用于 positiveMinutesBetween 流程中的校验、计算或对象转换。
     * @param requestedTo 时间或范围边界，用于限定统计窗口。
     * @return 返回 positive minutes between 计算得到的数量、金额或指标值。
     */
    private long positiveMinutesBetween(LocalDateTime availableUntil, LocalDateTime requestedTo) {
        if (availableUntil == null || requestedTo == null || !requestedTo.isAfter(availableUntil)) {
            return 0L;
        }
        return Math.max(0L, Duration.between(availableUntil, requestedTo).toMinutes());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param mode mode 参数，用于 modeCandidates 流程中的校验、计算或对象转换。
     * @return 返回 mode candidates 汇总后的集合、分页或映射视图。
     */
    private List<String> modeCandidates(String mode) {
        String normalized = normalizeMode(mode);
        if (MODE_HYBRID.equals(normalized)) {
            return List.of(MODE_HYBRID);
        }
        return List.of(normalized, MODE_HYBRID);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param candidateTenantId 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant rank 计算得到的数量、金额或指标值。
     */
    private int tenantRank(Long candidateTenantId, Long tenantId) {
        return normalizeTenant(candidateTenantId).equals(normalizeTenant(tenantId)) ? 1 : 0;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param candidateMode 时间参数，用于计算窗口、过期或审计时间。
     * @param mode mode 参数，用于 exactModeRank 流程中的校验、计算或对象转换。
     * @return 返回 exact mode rank 计算得到的数量、金额或指标值。
     */
    private int exactModeRank(String candidateMode, String mode) {
        return normalizeMode(candidateMode).equals(normalizeMode(mode)) ? 1 : 0;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant scope 汇总后的集合、分页或映射视图。
     */
    private List<Long> tenantScope(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (scopedTenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, scopedTenantId);
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
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param mode mode 参数，用于 normalizeMode 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeMode(String mode) {
        String value = hasText(mode) ? mode.trim().toUpperCase(Locale.ROOT) : MODE_HYBRID;
        if (!MODE_OFFLINE.equals(value) && !MODE_REALTIME.equals(value) && !MODE_HYBRID.equals(value)) {
            throw new IllegalArgumentException("mode must be OFFLINE, REALTIME, or HYBRID");
        }
        return value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param assetType 类型标识，用于选择对应处理分支。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeAssetType(String assetType) {
        String value = upperRequired(assetType, "assetType");
        if (!"TABLE".equals(value) && !"DATASET".equals(value) && !"METRIC".equals(value)) {
            throw new IllegalArgumentException("assetType must be TABLE, DATASET, or METRIC");
        }
        return value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeAvailabilityStatus(String status) {
        String value = upperRequired(status, "status");
        if (!STATUS_PASS.equals(value) && !STATUS_WARN.equals(value) && !STATUS_FAIL.equals(value)) {
            throw new IllegalArgumentException("status must be PASS, WARN, or FAIL");
        }
        return value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param gatePolicy gate policy 参数，用于 normalizeGatePolicy 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeGatePolicy(String gatePolicy) {
        String value = upperDefault(gatePolicy, GATE_BLOCK_ON_WARN);
        if (!GATE_BLOCK_ON_FAIL.equals(value) && !GATE_BLOCK_ON_WARN.equals(value)) {
            throw new IllegalArgumentException("gatePolicy must be BLOCK_ON_FAIL or BLOCK_ON_WARN");
        }
        return value;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 require non negative 计算得到的数量、金额或指标值。
     */
    private Integer requireNonNegative(Integer value, String fieldName) {
        int normalized = value == null ? 0 : value;
        if (normalized < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return normalized;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 requiredDate 流程生成的业务结果。
     */
    private LocalDateTime requiredDate(LocalDateTime value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 upper required 生成的文本或业务键。
     */
    private String upperRequired(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 upper default 生成的文本或业务键。
     */
    private String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limitMessage(String value) {
        if (value == null || value.length() <= MAX_MESSAGE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_MESSAGE_LENGTH);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe integer 计算得到的数量、金额或指标值。
     */
    private int safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 safe limit 计算得到的数量、金额或指标值。
     */
    private int safeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param values values 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    /**
     * AssetRef 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AssetRef(String assetType, String assetKey) {
    }

    /**
     * AssetAvailabilityCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * AssetAvailabilityView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * ConsumerAvailabilityContractCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * ConsumerAvailabilityContractView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * ConsumerAvailabilityEvaluation 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * AssetAvailabilityGate 承载对应领域的业务规则、流程编排和结果转换。
     */
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
