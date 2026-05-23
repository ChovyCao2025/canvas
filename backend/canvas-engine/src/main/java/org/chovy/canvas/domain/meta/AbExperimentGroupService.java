package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AbExperimentGroupService {

    private final AbExperimentGroupMapper mapper;

    public List<AbExperimentGroup> list(Long experimentId, boolean includeDisabled) {
        return mapper.selectList(new LambdaQueryWrapper<AbExperimentGroup>()
                .eq(AbExperimentGroup::getExperimentId, experimentId)
                .eq(!includeDisabled, AbExperimentGroup::getEnabled, 1)
                .orderByAsc(AbExperimentGroup::getSortOrder)
                .orderByAsc(AbExperimentGroup::getId));
    }

    public List<StubOption> activeGroupOptions(Long experimentId) {
        return list(experimentId, false).stream()
                .map(group -> new StubOption(group.getGroupKey(), group.getLabel()))
                .toList();
    }

    public void ensureDefaultGroups(Long experimentId) {
        insertDefaultIfMissing(experimentId, "A", "A组", 10);
        insertDefaultIfMissing(experimentId, "B", "B组", 20);
    }

    private void insertDefaultIfMissing(Long experimentId, String groupKey, String label, int sortOrder) {
        AbExperimentGroup existing = mapper.selectOne(new LambdaQueryWrapper<AbExperimentGroup>()
                .eq(AbExperimentGroup::getExperimentId, experimentId)
                .eq(AbExperimentGroup::getGroupKey, groupKey));
        if (existing != null) {
            return;
        }
        AbExperimentGroup group = new AbExperimentGroup();
        group.setExperimentId(experimentId);
        group.setGroupKey(groupKey);
        group.setLabel(label);
        group.setSortOrder(sortOrder);
        group.setEnabled(1);
        mapper.insert(group);
    }

    public AbExperimentGroup create(Long experimentId, AbExperimentGroup body) {
        validateGroupKey(body.getGroupKey());
        body.setExperimentId(experimentId);
        if (body.getEnabled() == null) {
            body.setEnabled(1);
        }
        if (body.getSortOrder() == null) {
            body.setSortOrder(0);
        }
        mapper.insert(body);
        return body;
    }

    public void update(Long experimentId, Long groupId, AbExperimentGroup body) {
        AbExperimentGroup existing = mapper.selectById(groupId);
        if (existing == null) {
            throw new IllegalArgumentException("AB 分组不存在: " + groupId);
        }
        if (!experimentId.equals(existing.getExperimentId())) {
            throw new IllegalArgumentException("AB 分组不属于当前实验: " + groupId);
        }
        if (body.getLabel() != null) {
            existing.setLabel(body.getLabel());
        }
        if (body.getSortOrder() != null) {
            existing.setSortOrder(body.getSortOrder());
        }
        if (body.getEnabled() != null) {
            existing.setEnabled(body.getEnabled());
        }
        mapper.updateById(existing);
    }

    public void disable(Long experimentId, Long groupId) {
        AbExperimentGroup existing = mapper.selectById(groupId);
        if (existing == null) {
            throw new IllegalArgumentException("AB 分组不存在: " + groupId);
        }
        if (!experimentId.equals(existing.getExperimentId())) {
            throw new IllegalArgumentException("AB 分组不属于当前实验: " + groupId);
        }
        existing.setEnabled(0);
        mapper.updateById(existing);
    }

    private void validateGroupKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("groupKey 只能包含字母、数字、下划线和中划线，长度 1-64");
        }
    }
}
