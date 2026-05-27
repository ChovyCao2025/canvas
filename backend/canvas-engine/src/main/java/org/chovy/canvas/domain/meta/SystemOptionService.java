package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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

    /** 查询指定分类下启用的选项。 */
    public List<StubOption> activeOptions(String category) {
        return activeSystemOptions(category).stream()
                .map(option -> new StubOption(option.getOptionKey(), option.getLabel()))
                .toList();
    }

    /** 查询指定分类下启用的系统内置选项。 */
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

    /** 更新允许后台编辑的系统选项字段。 */
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
