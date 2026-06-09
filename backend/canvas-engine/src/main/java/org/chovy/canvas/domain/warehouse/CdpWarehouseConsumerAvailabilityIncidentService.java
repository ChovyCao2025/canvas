package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
/**
 * CdpWarehouseConsumerAvailabilityIncidentService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseConsumerAvailabilityIncidentService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PASS = "PASS";

    private final CdpWarehouseConsumerAvailabilityService consumerAvailabilityService;
    private final CdpWarehouseIncidentService incidentService;

    /**
     * 初始化 CdpWarehouseConsumerAvailabilityIncidentService 实例。
     *
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseConsumerAvailabilityIncidentService(
            CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
            CdpWarehouseIncidentService incidentService) {
        this.consumerAvailabilityService = consumerAvailabilityService;
        this.incidentService = incidentService;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contractKey 业务键，用于在同一租户下定位资源。
     * @param consumerType 类型标识，用于选择对应处理分支。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String key : contractKeys) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contractKey 业务键，用于在同一租户下定位资源。
     * @param consumerType 类型标识，用于选择对应处理分支。
     * @return 返回 contract keys 汇总后的集合、分页或映射视图。
     */
    private List<String> contractKeys(Long tenantId, String contractKey, String consumerType) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(contractKey)) {
            return List.of(contractKey.trim());
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return safeList(consumerAvailabilityService.listContracts(
                        tenantId,
                        normalizeConsumerType(consumerType),
                        STATUS_ACTIVE))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractView::contractKey)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param evaluation evaluation 参数，用于 toIncidentInput 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param assetGates asset gates 参数，用于 assetGateSummaries 流程中的校验、计算或对象转换。
     * @return 返回 asset gate summaries 汇总后的集合、分页或映射视图。
     */
    private List<String> assetGateSummaries(
            List<CdpWarehouseConsumerAvailabilityService.AssetAvailabilityGate> assetGates) {
        return safeList(assetGates).stream()
                .map(gate -> gate.assetType()
                        + ":" + gate.assetKey()
                        + "=" + gate.status()
                        + "(" + gate.reason() + ")")
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param current current 参数，用于 worstStatus 流程中的校验、计算或对象转换。
     * @param next next 参数，用于 worstStatus 流程中的校验、计算或对象转换。
     * @return 返回 worst status 生成的文本或业务键。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : STATUS_PASS;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeConsumerType(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
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
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOperator(String operator) {
        return hasText(operator) ? operator.trim() : "consumer-availability-incident";
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param values values 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
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
     * ScanResult 承载对应领域的业务规则、流程编排和结果转换。
     */
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
