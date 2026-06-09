package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseSloPolicyDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseSloPolicyMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
/**
 * CdpWarehouseSloPolicyService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseSloPolicyService {

    public static final String DEFAULT_POLICY_KEY = "WAREHOUSE_READINESS_DEFAULT";

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int DEFAULT_OFFLINE_WARN_RUN_GAP_MINUTES = 120;
    private static final int DEFAULT_OFFLINE_FAIL_RUN_GAP_MINUTES = 360;
    private static final int DEFAULT_OFFLINE_WARN_WATERMARK_LAG_MINUTES = 30;
    private static final int DEFAULT_OFFLINE_FAIL_WATERMARK_LAG_MINUTES = 120;
    private static final int DEFAULT_AUDIENCE_WARN_RUN_GAP_MINUTES = 1440;
    private static final int DEFAULT_AUDIENCE_FAIL_RUN_GAP_MINUTES = 4320;

    private final CdpWarehouseSloPolicyMapper policyMapper;

    /**
     * 初始化 CdpWarehouseSloPolicyService 实例。
     *
     * @param policyMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseSloPolicyService(CdpWarehouseSloPolicyMapper policyMapper) {
        this.policyMapper = policyMapper;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<SloPolicyView> listPolicies(Long tenantId, String status) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehouseSloPolicyDO> query = new LambdaQueryWrapper<CdpWarehouseSloPolicyDO>()
                .in(CdpWarehouseSloPolicyDO::getTenantId, tenantScope(scopedTenantId))
                .orderByAsc(CdpWarehouseSloPolicyDO::getTenantId)
                .orderByAsc(CdpWarehouseSloPolicyDO::getPolicyKey);
        if (hasText(status)) {
            query.eq(CdpWarehouseSloPolicyDO::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        Map<String, SloPolicyView> byKey = new LinkedHashMap<>();
        for (CdpWarehouseSloPolicyDO row : safeList(policyMapper.selectList(query))) {
            byKey.put(row.getPolicyKey(), toView(row));
        }
        return new ArrayList<>(byKey.values());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 effectivePolicy 流程生成的业务结果。
     */
    public SloPolicyView effectivePolicy(Long tenantId) {
        return effectivePolicy(tenantId, DEFAULT_POLICY_KEY);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param policyKey 业务键，用于在同一租户下定位资源。
     * @return 返回 effectivePolicy 流程生成的业务结果。
     */
    public SloPolicyView effectivePolicy(Long tenantId, String policyKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedPolicyKey = defaultPolicyKey(policyKey);
        LambdaQueryWrapper<CdpWarehouseSloPolicyDO> query = new LambdaQueryWrapper<CdpWarehouseSloPolicyDO>()
                .in(CdpWarehouseSloPolicyDO::getTenantId, tenantScope(scopedTenantId))
                .eq(CdpWarehouseSloPolicyDO::getPolicyKey, scopedPolicyKey)
                .eq(CdpWarehouseSloPolicyDO::getStatus, STATUS_ACTIVE)
                .orderByAsc(CdpWarehouseSloPolicyDO::getTenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        SloPolicyView selected = null;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseSloPolicyDO row : safeList(policyMapper.selectList(query))) {
            selected = toView(row);
        }
        return selected == null ? defaultPolicy(scopedTenantId, scopedPolicyKey) : selected;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public SloPolicyView upsertPolicy(Long tenantId, SloPolicyCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("slo policy command is required");
        }
        CdpWarehouseSloPolicyDO row = new CdpWarehouseSloPolicyDO();
        row.setTenantId(normalizeTenant(tenantId));
        row.setPolicyKey(defaultPolicyKey(command.policyKey()));
        row.setDisplayName(defaultString(command.displayName(), row.getPolicyKey()));
        row.setOfflineWarnRunGapMinutes(positiveOrDefault(
                command.offlineWarnRunGapMinutes(), DEFAULT_OFFLINE_WARN_RUN_GAP_MINUTES,
                "offlineWarnRunGapMinutes"));
        row.setOfflineFailRunGapMinutes(positiveOrDefault(
                command.offlineFailRunGapMinutes(), DEFAULT_OFFLINE_FAIL_RUN_GAP_MINUTES,
                "offlineFailRunGapMinutes"));
        row.setOfflineWarnWatermarkLagMinutes(positiveOrDefault(
                command.offlineWarnWatermarkLagMinutes(), DEFAULT_OFFLINE_WARN_WATERMARK_LAG_MINUTES,
                "offlineWarnWatermarkLagMinutes"));
        row.setOfflineFailWatermarkLagMinutes(positiveOrDefault(
                command.offlineFailWatermarkLagMinutes(), DEFAULT_OFFLINE_FAIL_WATERMARK_LAG_MINUTES,
                "offlineFailWatermarkLagMinutes"));
        row.setAudienceWarnRunGapMinutes(positiveOrDefault(
                command.audienceWarnRunGapMinutes(), DEFAULT_AUDIENCE_WARN_RUN_GAP_MINUTES,
                "audienceWarnRunGapMinutes"));
        row.setAudienceFailRunGapMinutes(positiveOrDefault(
                command.audienceFailRunGapMinutes(), DEFAULT_AUDIENCE_FAIL_RUN_GAP_MINUTES,
                "audienceFailRunGapMinutes"));
        validateThresholdOrder(row.getOfflineWarnRunGapMinutes(), row.getOfflineFailRunGapMinutes(),
                "offline run gap");
        validateThresholdOrder(row.getOfflineWarnWatermarkLagMinutes(), row.getOfflineFailWatermarkLagMinutes(),
                "offline watermark lag");
        validateThresholdOrder(row.getAudienceWarnRunGapMinutes(), row.getAudienceFailRunGapMinutes(),
                "audience materialization run gap");
        row.setStatus(upperDefault(command.status(), STATUS_ACTIVE));
        row.setOwnerName(blankToNull(command.ownerName()));
        row.setDescription(blankToNull(command.description()));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        policyMapper.upsert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 defaultPolicy 流程生成的业务结果。
     */
    public static SloPolicyView defaultPolicy(Long tenantId) {
        return defaultPolicy(tenantId, DEFAULT_POLICY_KEY);
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param policyKey 业务键，用于在同一租户下定位资源。
     * @return 返回 defaultPolicy 流程生成的业务结果。
     */
    public static SloPolicyView defaultPolicy(Long tenantId, String policyKey) {
        return new SloPolicyView(
                null,
                tenantId == null ? 0L : tenantId,
                defaultStaticPolicyKey(policyKey),
                "Warehouse Readiness Default",
                DEFAULT_OFFLINE_WARN_RUN_GAP_MINUTES,
                DEFAULT_OFFLINE_FAIL_RUN_GAP_MINUTES,
                DEFAULT_OFFLINE_WARN_WATERMARK_LAG_MINUTES,
                DEFAULT_OFFLINE_FAIL_WATERMARK_LAG_MINUTES,
                DEFAULT_AUDIENCE_WARN_RUN_GAP_MINUTES,
                DEFAULT_AUDIENCE_FAIL_RUN_GAP_MINUTES,
                STATUS_ACTIVE,
                "data-platform",
                "In-code warehouse readiness default policy.");
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private SloPolicyView toView(CdpWarehouseSloPolicyDO row) {
        return new SloPolicyView(
                row.getId(),
                row.getTenantId(),
                row.getPolicyKey(),
                row.getDisplayName(),
                row.getOfflineWarnRunGapMinutes(),
                row.getOfflineFailRunGapMinutes(),
                row.getOfflineWarnWatermarkLagMinutes(),
                row.getOfflineFailWatermarkLagMinutes(),
                row.getAudienceWarnRunGapMinutes(),
                row.getAudienceFailRunGapMinutes(),
                row.getStatus(),
                row.getOwnerName(),
                row.getDescription());
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param warn warn 参数，用于 validateThresholdOrder 流程中的校验、计算或对象转换。
     * @param fail fail 参数，用于 validateThresholdOrder 流程中的校验、计算或对象转换。
     * @param label label 参数，用于 validateThresholdOrder 流程中的校验、计算或对象转换。
     */
    private void validateThresholdOrder(int warn, int fail, String label) {
        if (warn > fail) {
            throw new IllegalArgumentException(label + " warn threshold must be <= fail threshold");
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 positive or default 计算得到的数量、金额或指标值。
     */
    private int positiveOrDefault(Integer value, int defaultValue, String fieldName) {
        if (value == null) {
            return defaultValue;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default policy key 生成的文本或业务键。
     */
    private String defaultPolicyKey(String value) {
        return defaultStaticPolicyKey(value);
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default static policy key 生成的文本或业务键。
     */
    private static String defaultStaticPolicyKey(String value) {
        return hasStaticText(value) ? value.trim().toUpperCase(Locale.ROOT) : DEFAULT_POLICY_KEY;
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
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
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant scope 汇总后的集合、分页或映射视图。
     */
    private List<Long> tenantScope(Long tenantId) {
        if (tenantId == null || tenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, tenantId);
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
        return hasStaticText(value);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private static boolean hasStaticText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * SloPolicyCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record SloPolicyCommand(
            String policyKey,
            String displayName,
            Integer offlineWarnRunGapMinutes,
            Integer offlineFailRunGapMinutes,
            Integer offlineWarnWatermarkLagMinutes,
            Integer offlineFailWatermarkLagMinutes,
            Integer audienceWarnRunGapMinutes,
            Integer audienceFailRunGapMinutes,
            String status,
            String ownerName,
            String description) {
    }

    /**
     * SloPolicyView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record SloPolicyView(
            Long id,
            Long tenantId,
            String policyKey,
            String displayName,
            int offlineWarnRunGapMinutes,
            int offlineFailRunGapMinutes,
            int offlineWarnWatermarkLagMinutes,
            int offlineFailWatermarkLagMinutes,
            int audienceWarnRunGapMinutes,
            int audienceFailRunGapMinutes,
            String status,
            String ownerName,
            String description) {
    }
}
