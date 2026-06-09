package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import org.chovy.canvas.dal.dataobject.AbExperimentGroupDO;
import org.chovy.canvas.dal.mapper.AbExperimentGroupMapper;
import org.chovy.canvas.dto.StubOption;

/**
 * AB 实验 Group 元数据领域服务。
 *
 * <p>负责事件、接口、标签、系统选项或实验分组等配置型数据的维护和查询。
 * <p>元数据会影响画布运行时行为，因此该层需要兼顾管理端易用性与执行链路缓存一致性。
 */
@Service
@RequiredArgsConstructor
public class AbExperimentGroupService {

    /** AB 实验分组 Mapper，用于维护实验下的候选分组配置。 */
    private final AbExperimentGroupMapper mapper;

    /** 按条件查询列表数据。 */
    public List<AbExperimentGroupDO> list(Long experimentId, boolean includeDisabled) {
        return mapper.selectList(new LambdaQueryWrapper<AbExperimentGroupDO>()
                .eq(AbExperimentGroupDO::getExperimentId, experimentId)
                .eq(!includeDisabled, AbExperimentGroupDO::getEnabled, 1)
                .orderByAsc(AbExperimentGroupDO::getSortOrder)
                .orderByAsc(AbExperimentGroupDO::getId));
    }

    /** 查询启用的 AB 实验分组选项。 */
    public List<StubOption> activeGroupOptions(Long experimentId) {
        return list(experimentId, false).stream()
                .map(group -> new StubOption(group.getGroupKey(), group.getLabel()))
                .toList();
    }

    /** 为实验补齐默认 A/B 分组。 */
    public void ensureDefaultGroups(Long experimentId) {
        insertDefaultIfMissing(experimentId, "A", "A组", 10);
        insertDefaultIfMissing(experimentId, "B", "B组", 20);
    }

    /** 在实验缺少指定默认分组时创建分组，避免重复插入。 */
    private void insertDefaultIfMissing(Long experimentId, String groupKey, String label, int sortOrder) {
        AbExperimentGroupDO existing = mapper.selectOne(new LambdaQueryWrapper<AbExperimentGroupDO>()
                .eq(AbExperimentGroupDO::getExperimentId, experimentId)
                .eq(AbExperimentGroupDO::getGroupKey, groupKey));
        if (existing != null) {
            return;
        }
        AbExperimentGroupDO group = new AbExperimentGroupDO();
        group.setExperimentId(experimentId);
        group.setGroupKey(groupKey);
        group.setLabel(label);
        group.setSortOrder(sortOrder);
        group.setEnabled(1);
        mapper.insert(group);
    }

    /** 创建新记录，并执行必要的唯一性、格式和默认值处理。 */
    public AbExperimentGroupDO create(Long experimentId, AbExperimentGroupDO body) {
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

    /** 更新已有记录，仅修改允许变更的字段。 */
    public void update(Long experimentId, Long groupId, AbExperimentGroupDO body) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        AbExperimentGroupDO existing = mapper.selectById(groupId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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

    /** 禁用记录，使其不再参与后续业务流程。 */
    public void disable(Long experimentId, Long groupId) {
        AbExperimentGroupDO existing = mapper.selectById(groupId);
        if (existing == null) {
            throw new IllegalArgumentException("AB 分组不存在: " + groupId);
        }
        if (!experimentId.equals(existing.getExperimentId())) {
            throw new IllegalArgumentException("AB 分组不属于当前实验: " + groupId);
        }
        existing.setEnabled(0);
        mapper.updateById(existing);
    }

    /** 校验 AB 分组业务键格式，保证可用于前端配置和路由识别。 */
    private void validateGroupKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("groupKey 只能包含字母、数字、下划线和中划线，长度 1-64");
        }
    }
}
