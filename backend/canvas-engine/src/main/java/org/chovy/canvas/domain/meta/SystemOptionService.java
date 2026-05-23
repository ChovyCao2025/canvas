package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemOptionService {

    private final SystemOptionMapper mapper;

    public List<SystemOption> listForAdmin(String category, Integer enabled, String keyword) {
        LambdaQueryWrapper<SystemOption> wrapper = new LambdaQueryWrapper<SystemOption>()
                .eq(category != null && !category.isBlank(), SystemOption::getCategory, category)
                .eq(enabled != null, SystemOption::getEnabled, enabled)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SystemOption::getOptionKey, keyword)
                        .or()
                        .like(SystemOption::getLabel, keyword)
                        .or()
                        .like(SystemOption::getDescription, keyword))
                .orderByAsc(SystemOption::getCategory)
                .orderByAsc(SystemOption::getSortOrder)
                .orderByAsc(SystemOption::getId);
        return mapper.selectList(wrapper);
    }

    public List<StubOption> activeOptions(String category) {
        return activeSystemOptions(category).stream()
                .map(option -> new StubOption(option.getOptionKey(), option.getLabel()))
                .toList();
    }

    public List<SystemOption> activeSystemOptions(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category is required");
        }
        return mapper.selectList(new LambdaQueryWrapper<SystemOption>()
                .eq(SystemOption::getCategory, category)
                .eq(SystemOption::getEnabled, 1)
                .orderByAsc(SystemOption::getSortOrder)
                .orderByAsc(SystemOption::getId));
    }

    public void updateEditable(Long id, SystemOption patch) {
        SystemOption existing = mapper.selectById(id);
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
