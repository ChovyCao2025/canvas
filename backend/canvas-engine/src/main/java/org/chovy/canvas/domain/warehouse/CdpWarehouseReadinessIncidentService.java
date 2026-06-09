package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * CdpWarehouseReadinessIncidentService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseReadinessIncidentService {

    private static final String STATUS_PASS = "PASS";
    private static final String SECTION_INCIDENTS = "incidents";

    private final CdpWarehouseReadinessService readinessService;
    private final CdpWarehouseIncidentService incidentService;

    /**
     * 初始化 CdpWarehouseReadinessIncidentService 实例。
     *
     * @param readinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseReadinessIncidentService(CdpWarehouseReadinessService readinessService,
                                                CdpWarehouseIncidentService incidentService) {
        this.readinessService = readinessService;
        this.incidentService = incidentService;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回流程执行后的业务结果。
     */
    public ScanResult scan(Long tenantId) {
        CdpWarehouseReadinessService.ReadinessSummary readiness = readinessService.readiness(tenantId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (readiness == null) {
            return new ScanResult(normalizeTenant(tenantId), "UNKNOWN", 0, 0, 0, 0);
        }
        List<CdpWarehouseReadinessService.ReadinessSection> sections =
                readiness.sections() == null ? List.of() : readiness.sections();
        int opened = 0;
        int skipped = 0;
        int failed = 0;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseReadinessService.ReadinessSection section : sections) {
            if (shouldSkip(section)) {
                skipped++;
                continue;
            }
            try {
                incidentService.recordReadinessIncident(toIncidentInput(readiness, section));
                opened++;
            } catch (RuntimeException ignored) {
                failed++;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ScanResult(readiness.tenantId(), readiness.status(), sections.size(), opened, skipped, failed);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param readiness readiness 参数，用于 toIncidentInput 流程中的校验、计算或对象转换。
     * @param section section 参数，用于 toIncidentInput 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private CdpWarehouseIncidentService.ReadinessIncidentInput toIncidentInput(
            CdpWarehouseReadinessService.ReadinessSummary readiness,
            CdpWarehouseReadinessService.ReadinessSection section) {
        return new CdpWarehouseIncidentService.ReadinessIncidentInput(
                readiness.tenantId(),
                section.key(),
                readiness.status(),
                section.status(),
                section.reason(),
                readiness.generatedAt());
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param section section 参数，用于 shouldSkip 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean shouldSkip(CdpWarehouseReadinessService.ReadinessSection section) {
        if (section == null || !hasText(section.status())) {
            return true;
        }
        if (STATUS_PASS.equalsIgnoreCase(section.status().trim())) {
            return true;
        }
        return hasText(section.key()) && SECTION_INCIDENTS.equalsIgnoreCase(section.key().trim());
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
            String readinessStatus,
            int totalSections,
            int opened,
            int skipped,
            int failed) {
    }
}
