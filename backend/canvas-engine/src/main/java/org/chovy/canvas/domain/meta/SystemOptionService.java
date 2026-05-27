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

@Service
@RequiredArgsConstructor
public class SystemOptionService {

    private final SystemOptionMapper mapper;

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

    public List<StubOption> activeOptions(String category) {
        return activeOptions(category, null);
    }

    public List<StubOption> activeOptions(String category, Long tenantId) {
        return activeSystemOptions(category, tenantId).stream()
                .map(option -> new StubOption(option.getOptionKey(), option.getLabel()))
                .toList();
    }

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
