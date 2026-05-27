package org.chovy.canvas.domain.tenant;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.dataobject.TenantDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.TenantMapper;
import org.chovy.canvas.dto.tenant.TenantUsageDTO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TenantService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";

    private static final String DEFAULT_PLAN_CODE = "default";
    private static final String TENANT_KEY_PATTERN = "[a-z0-9_-]+";

    private final TenantMapper tenantMapper;
    private final CanvasMapper canvasMapper;
    private final CanvasExecutionMapper executionMapper;
    private final CanvasExecutionDlqMapper dlqMapper;

    public TenantDO create(String name, String tenantKey, String planCode, String quotaJson, String operator) {
        TenantDO tenant = new TenantDO();
        tenant.setName(requireText(name, "租户名称"));
        tenant.setTenantKey(normalizeTenantKey(tenantKey));
        tenant.setStatus(STATUS_ACTIVE);
        tenant.setPlanCode(defaultIfBlank(planCode, DEFAULT_PLAN_CODE));
        tenant.setQuotaJson(blankToNull(quotaJson));
        tenant.setCreatedBy(blankToNull(operator));
        try {
            tenantMapper.insert(tenant);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("租户 key 已存在: " + tenant.getTenantKey(), e);
        }
        return tenant;
    }

    public void disable(Long id, String operator) {
        updateStatus(id, STATUS_DISABLED, operator);
    }

    public void activate(Long id, String operator) {
        updateStatus(id, STATUS_ACTIVE, operator);
    }

    public TenantUsageDTO usage(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("租户 ID 不能为空");
        }

        TenantUsageDTO usage = new TenantUsageDTO();
        usage.setTenantId(tenantId);
        usage.setCanvasCount(count(canvasMapper.selectCount(
                new QueryWrapper<CanvasDO>().eq("tenant_id", tenantId))));
        usage.setPublishedCanvasCount(count(canvasMapper.selectCount(
                new QueryWrapper<CanvasDO>()
                        .eq("tenant_id", tenantId)
                        .eq("status", CanvasStatusEnum.PUBLISHED.getCode()))));
        usage.setExecutionCount(count(executionMapper.selectCount(
                new QueryWrapper<CanvasExecutionDO>().eq("tenant_id", tenantId))));
        usage.setFailedExecutionCount(count(executionMapper.selectCount(
                new QueryWrapper<CanvasExecutionDO>()
                        .eq("tenant_id", tenantId)
                        .eq("status", ExecutionStatus.FAILED.getCode()))));
        usage.setDlqCount(count(dlqMapper.selectCount(
                new QueryWrapper<CanvasExecutionDlqDO>()
                        .apply("canvas_id IN (SELECT id FROM canvas WHERE tenant_id = {0})", tenantId))));
        return usage;
    }

    public List<TenantDO> list() {
        return tenantMapper.selectList(null);
    }

    private void updateStatus(Long id, String status, String operator) {
        if (id == null) {
            throw new IllegalArgumentException("租户 ID 不能为空");
        }
        TenantDO tenant = new TenantDO();
        tenant.setId(id);
        tenant.setStatus(status);
        tenant.setUpdatedBy(blankToNull(operator));
        tenantMapper.updateById(tenant);
    }

    private String normalizeTenantKey(String tenantKey) {
        String normalized = requireText(tenantKey, "租户 key").toLowerCase(Locale.ROOT);
        if (!normalized.matches(TENANT_KEY_PATTERN)) {
            throw new IllegalArgumentException("租户 key 只能包含小写字母、数字、_、-");
        }
        return normalized;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private long count(Long value) {
        return value == null ? 0L : value;
    }
}
