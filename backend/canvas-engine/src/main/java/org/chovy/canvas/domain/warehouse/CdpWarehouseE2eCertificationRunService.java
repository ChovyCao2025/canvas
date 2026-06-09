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
/**
 * CdpWarehouseE2eCertificationRunService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseE2eCertificationRunService {

    private static final String STATUS_FAIL = "FAIL";

    private final CdpWarehousePhysicalE2eCertificationService certificationService;
    private final CdpWarehouseE2eCertificationRunMapper runMapper;
    private final ObjectMapper objectMapper;

    /**
     * 初始化 CdpWarehouseE2eCertificationRunService 实例。
     *
     * @param certificationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseE2eCertificationRunService(
            CdpWarehousePhysicalE2eCertificationService certificationService,
            CdpWarehouseE2eCertificationRunMapper runMapper) {
        this.certificationService = certificationService;
        this.runMapper = runMapper;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 run 流程中的校验、计算或对象转换。
     * @param contractKeys contract keys 参数，用于 run 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 run 流程中的校验、计算或对象转换。
     * @param requestedBy requested by 参数，用于 run 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public CertificationRunView run(Long tenantId,
                                    LocalDateTime from,
                                    LocalDateTime to,
                                    String mode,
                                    List<String> contractKeys,
                                    boolean requirePhysical,
                                    String requestedBy) {
        return run(tenantId, from, to, mode, contractKeys, requirePhysical, false, requestedBy);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 run 流程中的校验、计算或对象转换。
     * @param contractKeys contract keys 参数，用于 run 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 run 流程中的校验、计算或对象转换。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @param requestedBy requested by 参数，用于 run 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 run 流程中的校验、计算或对象转换。
     * @param contractKeys contract keys 参数，用于 run 流程中的校验、计算或对象转换。
     * @param requirePhysical require physical 参数，用于 run 流程中的校验、计算或对象转换。
     * @param requireRealtime 时间参数，用于计算窗口、过期或审计时间。
     * @param requireDataPathProof require data path proof 参数，用于 run 流程中的校验、计算或对象转换。
     * @param requestedBy requested by 参数，用于 run 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public CertificationRunView run(Long tenantId,
                                    LocalDateTime from,
                                    LocalDateTime to,
                                    String mode,
                                    List<String> contractKeys,
                                    boolean requirePhysical,
                                    boolean requireRealtime,
                                    boolean requireDataPathProof,
                                    String requestedBy) {
        // 准备本次处理所需的上下文和中间变量。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        runMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 get 流程生成的业务结果。
     */
    public CertificationRunView get(Long tenantId, Long id) {
        CdpWarehouseE2eCertificationRunDO row = runMapper.selectById(id);
        if (row == null || !normalizeTenant(tenantId).equals(row.getTenantId())) {
            throw new IllegalArgumentException("certification run not found: " + id);
        }
        return toView(row);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private CertificationRunView toView(CdpWarehouseE2eCertificationRunDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize certification run evidence", ex);
        }
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param ex ex 参数，用于 message 流程中的校验、计算或对象转换。
     * @return 返回 message 生成的文本或业务键。
     */
    private String message(RuntimeException ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    /**
     * CertificationRunView 承载对应领域的业务规则、流程编排和结果转换。
     */
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
