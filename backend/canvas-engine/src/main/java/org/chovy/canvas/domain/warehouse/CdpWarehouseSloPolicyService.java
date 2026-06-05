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

    public CdpWarehouseSloPolicyService(CdpWarehouseSloPolicyMapper policyMapper) {
        this.policyMapper = policyMapper;
    }

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

    public SloPolicyView effectivePolicy(Long tenantId) {
        return effectivePolicy(tenantId, DEFAULT_POLICY_KEY);
    }

    public SloPolicyView effectivePolicy(Long tenantId, String policyKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedPolicyKey = defaultPolicyKey(policyKey);
        LambdaQueryWrapper<CdpWarehouseSloPolicyDO> query = new LambdaQueryWrapper<CdpWarehouseSloPolicyDO>()
                .in(CdpWarehouseSloPolicyDO::getTenantId, tenantScope(scopedTenantId))
                .eq(CdpWarehouseSloPolicyDO::getPolicyKey, scopedPolicyKey)
                .eq(CdpWarehouseSloPolicyDO::getStatus, STATUS_ACTIVE)
                .orderByAsc(CdpWarehouseSloPolicyDO::getTenantId);
        SloPolicyView selected = null;
        for (CdpWarehouseSloPolicyDO row : safeList(policyMapper.selectList(query))) {
            selected = toView(row);
        }
        return selected == null ? defaultPolicy(scopedTenantId, scopedPolicyKey) : selected;
    }

    public SloPolicyView upsertPolicy(Long tenantId, SloPolicyCommand command) {
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
        policyMapper.upsert(row);
        return toView(row);
    }

    public static SloPolicyView defaultPolicy(Long tenantId) {
        return defaultPolicy(tenantId, DEFAULT_POLICY_KEY);
    }

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

    private void validateThresholdOrder(int warn, int fail, String label) {
        if (warn > fail) {
            throw new IllegalArgumentException(label + " warn threshold must be <= fail threshold");
        }
    }

    private int positiveOrDefault(Integer value, int defaultValue, String fieldName) {
        if (value == null) {
            return defaultValue;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private String defaultPolicyKey(String value) {
        return defaultStaticPolicyKey(value);
    }

    private static String defaultStaticPolicyKey(String value) {
        return hasStaticText(value) ? value.trim().toUpperCase(Locale.ROOT) : DEFAULT_POLICY_KEY;
    }

    private String defaultString(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private List<Long> tenantScope(Long tenantId) {
        if (tenantId == null || tenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, tenantId);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private boolean hasText(String value) {
        return hasStaticText(value);
    }

    private static boolean hasStaticText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

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
