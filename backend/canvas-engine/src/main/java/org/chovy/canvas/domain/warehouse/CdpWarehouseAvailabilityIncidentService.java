package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
/**
 * CdpWarehouseAvailabilityIncidentService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseAvailabilityIncidentService {

    private static final String STATUS_PASS = "PASS";

    private final CdpWarehouseAvailabilityService availabilityService;
    private final CdpWarehouseIncidentService incidentService;

    /**
     * 初始化 CdpWarehouseAvailabilityIncidentService 实例。
     *
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseAvailabilityIncidentService(CdpWarehouseAvailabilityService availabilityService,
                                                   CdpWarehouseIncidentService incidentService) {
        this.availabilityService = availabilityService;
        this.incidentService = incidentService;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 scan 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    public ScanResult scan(Long tenantId,
                           LocalDateTime from,
                           LocalDateTime to,
                           String mode,
                           String operator) {
        CdpWarehouseAvailabilityService.AvailabilityDecision decision =
                availabilityService.evaluate(tenantId, from, to, mode);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param decision decision 参数，用于 toIncidentInput 流程中的校验、计算或对象转换。
     * @param gate gate 参数，用于 toIncidentInput 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOperator(String operator) {
        return operator == null || operator.isBlank() ? "warehouse-availability" : operator.trim();
    }

    /**
     * ScanResult 承载对应领域的业务规则、流程编排和结果转换。
     */
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
