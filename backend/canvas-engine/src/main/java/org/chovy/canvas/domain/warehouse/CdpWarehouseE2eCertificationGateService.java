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
/**
 * CdpWarehouseE2eCertificationGateService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseE2eCertificationGateService {

    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_FAIL = "FAIL";
    private static final int RECENT_LIMIT = 100;

    private final CdpWarehouseE2eCertificationRunMapper runMapper;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    /**
     * 初始化 CdpWarehouseE2eCertificationGateService 实例。
     *
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseE2eCertificationGateService(CdpWarehouseE2eCertificationRunMapper runMapper) {
        this(runMapper, Clock.systemDefaultZone());
    }

    /**
     * 初始化 CdpWarehouseE2eCertificationGateService 实例。
     *
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    CdpWarehouseE2eCertificationGateService(CdpWarehouseE2eCertificationRunMapper runMapper, Clock clock) {
        this.runMapper = runMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param mode mode 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param contractKeys contract keys 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param maxAgeMinutes max age minutes 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    public GateDecision evaluate(Long tenantId,
                                 String mode,
                                 List<String> contractKeys,
                                 boolean requirePhysical,
                                 long maxAgeMinutes) {
        return evaluate(tenantId, mode, contractKeys, requirePhysical, false, maxAgeMinutes);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param mode mode 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param contractKeys contract keys 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @param maxAgeMinutes max age minutes 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    public GateDecision evaluate(Long tenantId,
                                 String mode,
                                 List<String> contractKeys,
                                 boolean requirePhysical,
                                 boolean requireRealtime,
                                 long maxAgeMinutes) {
        return evaluate(tenantId, mode, contractKeys, requirePhysical, requireRealtime, false, maxAgeMinutes);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param mode mode 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param contractKeys contract keys 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @param requireDataPathProof require data path proof 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param maxAgeMinutes max age minutes 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @return 返回 evaluate 流程生成的业务结果。
     */
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
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (matched == null) {
            return decision(scopedTenantId, STATUS_FAIL, "no matching certification run",
                    null, null, null, null, normalizedMode, requirePhysical, requireRealtime,
                    requireDataPathProof, safeMaxAge, requiredContracts);
        }

        LocalDateTime finishedAt = matched.getFinishedAt();
        LocalDateTime expiresAt = finishedAt == null ? null : finishedAt.plusMinutes(safeMaxAge);
        List<String> runContracts = safeContractKeys(parseContractKeys(matched.getContractKeysJson()));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return decision(scopedTenantId, STATUS_PASS,
                "fresh PASS certification evidence",
                matched.getId(), matched.getStatus(), finishedAt, expiresAt,
                normalizedMode, requirePhysical, requireRealtime, requireDataPathProof, safeMaxAge, requiredContracts);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param mode mode 参数，用于 latestMatchingRun 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 latestMatchingRun 流程中的校验、计算或对象转换。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @param requireDataPathProof require data path proof 参数，用于 latestMatchingRun 流程中的校验、计算或对象转换。
     * @return 返回 latestMatchingRun 流程生成的业务结果。
     */
    private CdpWarehouseE2eCertificationRunDO latestMatchingRun(Long tenantId,
                                                                String mode,
                                                                boolean requirePhysical,
                                                                boolean requireRealtime,
                                                                boolean requireDataPathProof) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<CdpWarehouseE2eCertificationRunDO> rows = runMapper.selectList(
                new LambdaQueryWrapper<CdpWarehouseE2eCertificationRunDO>()
                        .eq(CdpWarehouseE2eCertificationRunDO::getTenantId, tenantId)
                        .orderByDesc(CdpWarehouseE2eCertificationRunDO::getId)
                        .last("LIMIT " + RECENT_LIMIT));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (rows == null) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return rows.stream()
                .filter(row -> mode.equals(normalizeMode(row.getMode())))
                .filter(row -> !requirePhysical || Integer.valueOf(1).equals(row.getRequirePhysical()))
                .filter(row -> !requireRealtime || Integer.valueOf(1).equals(row.getRequireRealtime()))
                .filter(row -> !requireDataPathProof || Integer.valueOf(1).equals(row.getRequireDataPathProof()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回解析、归一化或安全处理后的值。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dataPathProofJson JSON 字符串，承载结构化配置或明细。
     * @return 返回 realtime data path source failure reason 生成的文本或业务键。
     */
    private String realtimeDataPathSourceFailureReason(String dataPathProofJson) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (dataPathProofJson == null || dataPathProofJson.isBlank()) {
            return "realtime certification requires dataPathProof sourceMode MYSQL_CDC but proof is missing";
        }
        try {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return "realtime certification requires dataPathProof sourceMode MYSQL_CDC but proof JSON is invalid";
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param node node 参数，用于 text 流程中的校验、计算或对象转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 text 生成的文本或业务键。
     */
    private String text(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        return node.get(fieldName).asText();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatusLike(String value) {
        return value == null || value.isBlank()
                ? ""
                : value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 default text 生成的文本或业务键。
     */
    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param matchedRunId 业务对象 ID，用于定位具体记录。
     * @param matchedRunStatus 业务状态，用于筛选或推进状态流转。
     * @param matchedFinishedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param expiresAt 时间参数，用于计算窗口、过期或审计时间。
     * @param mode mode 参数，用于 decision 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 decision 流程中的校验、计算或对象转换。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @param requireDataPathProof require data path proof 参数，用于 decision 流程中的校验、计算或对象转换。
     * @param maxAgeMinutes max age minutes 参数，用于 decision 流程中的校验、计算或对象转换。
     * @param contractKeys contract keys 参数，用于 decision 流程中的校验、计算或对象转换。
     * @return 返回 decision 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param contractKeys contract keys 参数，用于 safeContractKeys 流程中的校验、计算或对象转换。
     * @return 返回 safe contract keys 汇总后的集合、分页或映射视图。
     */
    private List<String> safeContractKeys(List<String> contractKeys) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (contractKeys == null) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return contractKeys.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
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
        return mode == null || mode.isBlank() ? "HYBRID" : mode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? STATUS_FAIL : status.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * GateDecision 承载对应领域的业务规则、流程编排和结果转换。
     */
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
