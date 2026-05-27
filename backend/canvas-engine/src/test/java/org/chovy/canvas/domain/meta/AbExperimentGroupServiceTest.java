package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.chovy.canvas.dal.dataobject.AbExperimentGroupDO;
import org.chovy.canvas.dal.mapper.AbExperimentGroupMapper;
import org.chovy.canvas.dto.StubOption;

/**
 * AB 实验 Group 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class AbExperimentGroupServiceTest {

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                AbExperimentGroupDO.class);
    }

    @Test
    void createDefaultsCreatesAAndB() {
        AbExperimentGroupMapper mapper = mock(AbExperimentGroupMapper.class);
        AbExperimentGroupService service = new AbExperimentGroupService(mapper);

        service.ensureDefaultGroups(12L);

        verify(mapper, times(2)).insert(argThat((AbExperimentGroupDO group) ->
                group.getExperimentId().equals(12L)
                        && List.of("A", "B").contains(group.getGroupKey())
                        && group.getEnabled() == 1));
    }

    @Test
    void activeOptionsMapsGroupKeyToLabel() {
        AbExperimentGroupMapper mapper = mock(AbExperimentGroupMapper.class);
        AbExperimentGroupDO group = new AbExperimentGroupDO();
        group.setGroupKey("A");
        group.setLabel("A组");
        when(mapper.selectList(any())).thenReturn(List.of(group));
        AbExperimentGroupService service = new AbExperimentGroupService(mapper);

        assertThat(service.activeGroupOptions(1L))
                .extracting(StubOption::getKey)
                .containsExactly("A");
    }

    @Test
    void updateRejectsMissingGroup() {
        AbExperimentGroupMapper mapper = mock(AbExperimentGroupMapper.class);
        when(mapper.selectById(99L)).thenReturn(null);
        AbExperimentGroupService service = new AbExperimentGroupService(mapper);

        assertThatThrownBy(() -> service.update(1L, 99L, new AbExperimentGroupDO()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AB 分组不存在");
    }

    @Test
    void updateRejectsGroupFromOtherExperiment() {
        AbExperimentGroupMapper mapper = mock(AbExperimentGroupMapper.class);
        AbExperimentGroupDO existing = new AbExperimentGroupDO();
        existing.setId(99L);
        existing.setExperimentId(2L);
        when(mapper.selectById(99L)).thenReturn(existing);
        AbExperimentGroupService service = new AbExperimentGroupService(mapper);

        assertThatThrownBy(() -> service.update(1L, 99L, new AbExperimentGroupDO()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不属于当前实验");
    }
}
