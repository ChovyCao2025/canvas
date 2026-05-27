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

    public List<SystemOptionDO> listForAdmin(String category, Integer enabled, String keyword,
                                             Long tenantId, boolean superAdmin) {
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
        return mapper.selectList(wrapper);
    }

    /** 查询指定分类下启用的选项。 */
    public List<StubOption> activeOptions(String category) {
        return activeOptions(category, null);
    }

    public List<StubOption> activeOptions(String category, Long tenantId) {
        return activeSystemOptions(category, tenantId).stream()
                .map(option -> new StubOption(option.getOptionKey(), option.getLabel()))
                .toList();
    }

    /** 查询指定分类下启用的系统内置选项。 */
    public List<SystemOptionDO> activeSystemOptions(String category) {
        return activeSystemOptions(category, null);
    }

    public List<SystemOptionDO> activeSystemOptions(String category, Long tenantId) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category is required");
        }
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
        for (SystemOptionDO option : rows) {
            dedup.putIfAbsent(option.getOptionKey(), option);
        }
        return new ArrayList<>(dedup.values());
    }

    /** 更新允许后台编辑的系统选项字段。 */
    public void updateEditable(Long id, SystemOptionDO patch) {
        updateEditable(id, patch, null);
    }

    public void updateEditable(Long id, SystemOptionDO patch, TenantContext operator) {
        SystemOptionDO existing = mapper.selectById(id);
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

    private Long requireTenantId(Long tenantId) {
        if (tenantId == null) {
            throw new AccessDeniedException("当前用户缺少租户 ID");
        }
        return tenantId;
    }
}
