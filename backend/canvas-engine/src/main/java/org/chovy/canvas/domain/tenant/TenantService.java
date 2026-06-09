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

/**
 * TenantService 编排 domain.tenant 场景的领域业务规则。
 */
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

    /**
     * 创建新租户并初始化基础套餐信息。
     * tenantKey 会被规范化为小写并校验唯一性，重复 key 会转为业务异常。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("租户 key 已存在: " + tenant.getTenantKey(), e);
        }
        return tenant;
    }

    /**
     * 禁用指定租户。
     * 方法只更新租户状态和操作者，不会删除租户下画布、执行记录或其它业务数据。
     */
    public void disable(Long id, String operator) {
        updateStatus(id, STATUS_DISABLED, operator);
    }

    /**
     * 重新启用指定租户。
     * 方法把租户状态恢复为 ACTIVE，并记录更新操作者。
     */
    public void activate(Long id, String operator) {
        updateStatus(id, STATUS_ACTIVE, operator);
    }

    /**
     * 汇总租户资源使用情况。
     * 返回画布数、已发布画布数、执行数、失败执行数和 DLQ 数，用于租户管理和配额展示。
     */
    public TenantUsageDTO usage(Long tenantId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (tenantId == null) {
            throw new IllegalArgumentException("租户 ID 不能为空");
        }

        TenantUsageDTO usage = new TenantUsageDTO();
        usage.setTenantId(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return usage;
    }

    /**
     * 查询全部租户。
     * 该方法不做分页和权限过滤，调用方应在控制层限制为平台管理入口。
     */
    public List<TenantDO> list() {
        return tenantMapper.selectList(null);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param operator 操作人标识，用于审计和权限判断。
     */
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

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantKey 业务键，用于在同一租户下定位资源。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeTenantKey(String tenantKey) {
        String normalized = requireText(tenantKey, "租户 key").toLowerCase(Locale.ROOT);
        if (!normalized.matches(TENANT_KEY_PATTERN)) {
            throw new IllegalArgumentException("租户 key 只能包含小写字母、数字、_、-");
        }
        return normalized;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 require text 生成的文本或业务键。
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 default if blank 生成的文本或业务键。
     */
    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 统计符合条件的数据规模或状态数量。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回统计数量。
     */
    private long count(Long value) {
        return value == null ? 0L : value;
    }
}
