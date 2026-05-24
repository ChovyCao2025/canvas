package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import org.chovy.canvas.dal.dataobject.SystemOptionDO;
import org.chovy.canvas.dal.mapper.SystemOptionMapper;
import org.chovy.canvas.dto.StubOption;

@Service
@RequiredArgsConstructor
public class SystemOptionService {

    private final SystemOptionMapper mapper;

    public List<SystemOptionDO> listForAdmin(String category, Integer enabled, String keyword) {
        LambdaQueryWrapper<SystemOptionDO> wrapper = new LambdaQueryWrapper<SystemOptionDO>()
                .eq(category != null && !category.isBlank(), SystemOptionDO::getCategory, category)
                .eq(enabled != null, SystemOptionDO::getEnabled, enabled)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SystemOptionDO::getOptionKey, keyword)
                        .or()
                        .like(SystemOptionDO::getLabel, keyword)
                        .or()
                        .like(SystemOptionDO::getDescription, keyword))
                .orderByAsc(SystemOptionDO::getCategory)
                .orderByAsc(SystemOptionDO::getSortOrder)
                .orderByAsc(SystemOptionDO::getId);
        return mapper.selectList(wrapper);
    }

    public List<StubOption> activeOptions(String category) {
        return activeSystemOptions(category).stream()
                .map(option -> new StubOption(option.getOptionKey(), option.getLabel()))
                .toList();
    }

    public List<SystemOptionDO> activeSystemOptions(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category is required");
        }
        return mapper.selectList(new LambdaQueryWrapper<SystemOptionDO>()
                .eq(SystemOptionDO::getCategory, category)
                .eq(SystemOptionDO::getEnabled, 1)
                .orderByAsc(SystemOptionDO::getSortOrder)
                .orderByAsc(SystemOptionDO::getId));
    }

    public void updateEditable(Long id, SystemOptionDO patch) {
        SystemOptionDO existing = mapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("系统选项不存在: " + id);
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
}
