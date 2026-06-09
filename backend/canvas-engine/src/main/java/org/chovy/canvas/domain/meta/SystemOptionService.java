package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.chovy.canvas.dal.dataobject.SystemOptionDO;
import org.chovy.canvas.dal.mapper.SystemOptionMapper;
import org.chovy.canvas.dto.StubOption;

/**
 * 系统选项 元数据领域服务。
 *
 * <p>负责事件、接口、标签、系统选项或实验分组等配置型数据的维护和查询。
 * <p>元数据会影响画布运行时行为，因此该层需要兼顾管理端易用性与执行链路缓存一致性。
 */
@Service
@RequiredArgsConstructor
public class SystemOptionService {

    /** 系统选项 Mapper，用于管理后台配置项和运行时可选值。 */
    private final SystemOptionMapper mapper;

    /** 管理端按分类、启用状态和关键字查询系统选项列表。 */
    public List<SystemOptionDO> listForAdmin(String category, Integer enabled, String keyword) {
        return listForAdmin(category, enabled, keyword, null, true);
    }

    /**
     * 管理端查询系统选项，并根据操作者身份控制租户可见范围。
     * 超级管理员可查看全局和指定租户选项，普通租户管理员只能查看全局加本租户选项。
     */
    public List<SystemOptionDO> listForAdmin(String category, Integer enabled, String keyword,
                                             Long tenantId, boolean superAdmin) {
        // 准备本次处理所需的上下文和中间变量。
        LambdaQueryWrapper<SystemOptionDO> wrapper = new LambdaQueryWrapper<SystemOptionDO>()
                .eq(category != null && !category.isBlank(), SystemOptionDO::getCategory, category)
                .eq(enabled != null, SystemOptionDO::getEnabled, enabled)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SystemOptionDO::getOptionKey, keyword)
                        .or()
                        .like(SystemOptionDO::getLabel, keyword)
                        .or()
                        .like(SystemOptionDO::getDescription, keyword))
                .orderByDesc(SystemOptionDO::getTenantId)
                .orderByAsc(SystemOptionDO::getCategory)
                .orderByAsc(SystemOptionDO::getSortOrder)
                .orderByAsc(SystemOptionDO::getId);
        if (superAdmin) {
            if (tenantId != null) {
                wrapper.and(w -> w.isNull(SystemOptionDO::getTenantId)
                        .or()
                        .eq(SystemOptionDO::getTenantId, tenantId));
            }
        } else {
            Long normalizedTenantId = requireTenantId(tenantId);
            wrapper.and(w -> w.isNull(SystemOptionDO::getTenantId)
                    .or()
                    .eq(SystemOptionDO::getTenantId, normalizedTenantId));
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return mapper.selectList(wrapper);
    }

    /** 查询指定分类下启用的选项。 */
    public List<StubOption> activeOptions(String category) {
        return activeOptions(category, null);
    }

    /**
     * 查询指定分类在租户视角下启用的轻量选项。
     * 返回只包含 key 和 label 的 StubOption，适合前端下拉框使用。
     */
    public List<StubOption> activeOptions(String category, Long tenantId) {
        return activeSystemOptions(category, tenantId).stream()
                .map(option -> new StubOption(option.getOptionKey(), option.getLabel()))
                .toList();
    }

    /** 查询指定分类下启用的系统内置选项。 */
    public List<SystemOptionDO> activeSystemOptions(String category) {
        return activeSystemOptions(category, null);
    }

    /**
     * 查询指定分类下启用的系统选项实体。
     * 传入 tenantId 时会同时读取全局和租户覆盖项，并按 optionKey 去重优先保留租户项。
     */
    public List<SystemOptionDO> activeSystemOptions(String category, Long tenantId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category is required");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<SystemOptionDO> rows = mapper.selectList(new LambdaQueryWrapper<SystemOptionDO>()
                .eq(SystemOptionDO::getCategory, category)
                .eq(SystemOptionDO::getEnabled, 1)
                .and(tenantId != null, w -> w.isNull(SystemOptionDO::getTenantId)
                        .or()
                        .eq(SystemOptionDO::getTenantId, tenantId))
                .orderByDesc(SystemOptionDO::getTenantId)
                .orderByAsc(SystemOptionDO::getSortOrder)
                .orderByAsc(SystemOptionDO::getId));
        if (tenantId == null) {
            return rows;
        }
        Map<String, SystemOptionDO> dedup = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (SystemOptionDO option : rows) {
            dedup.putIfAbsent(option.getOptionKey(), option);
        }
        return new ArrayList<>(dedup.values());
    }

    /** 更新允许后台编辑的系统选项字段。 */
    public void updateEditable(Long id, SystemOptionDO patch) {
        updateEditable(id, patch, null);
    }

    /**
     * 更新系统选项中允许后台编辑的字段。
     * 非超级管理员只能编辑本租户选项；方法只应用 label、description、sortOrder 和 enabled，不允许改 key 或分类。
     */
    public void updateEditable(Long id, SystemOptionDO patch, TenantContext operator) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        SystemOptionDO existing = mapper.selectById(id);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (existing == null) {
            throw new IllegalArgumentException("系统选项不存在: " + id);
        }
        if (operator != null && !operator.isSuperAdmin()) {
            Long operatorTenantId = requireTenantId(operator.tenantId());
            if (existing.getTenantId() == null || !operatorTenantId.equals(existing.getTenantId())) {
                throw new AccessDeniedException("只能编辑当前租户的系统选项");
            }
        }
        if (patch.getLabel() != null) {
            existing.setLabel(patch.getLabel());
        }
        existing.setDescription(patch.getDescription());
        if (patch.getSortOrder() != null) {
            existing.setSortOrder(patch.getSortOrder());
        }
        if (patch.getEnabled() != null) {
            existing.setEnabled(patch.getEnabled());
        }
        mapper.updateById(existing);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 require tenant id 计算得到的数量、金额或指标值。
     */
    private Long requireTenantId(Long tenantId) {
        if (tenantId == null) {
            throw new AccessDeniedException("当前用户缺少租户 ID");
        }
        return tenantId;
    }
}
