package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.chovy.canvas.dal.dataobject.SystemOptionDO;
import org.chovy.canvas.dal.mapper.SystemOptionMapper;
import org.chovy.canvas.dto.StubOption;

/**
 * 系统选项 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class SystemOptionServiceTest {

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                SystemOptionDO.class);
    }

    @Test
    void activeOptionsReturnsStubOptionsInMapperOrder() {
        SystemOptionMapper mapper = mock(SystemOptionMapper.class);
        SystemOptionDO option = new SystemOptionDO();
        option.setOptionKey("EQ");
        option.setLabel("等于");
        when(mapper.selectList(any())).thenReturn(List.of(option));

        SystemOptionService service = new SystemOptionService(mapper);

        List<StubOption> options = service.activeOptions("condition_operator");

        assertThat(options).extracting(StubOption::getKey).containsExactly("EQ");
        assertThat(options).extracting(StubOption::getLabel).containsExactly("等于");
    }

    @Test
    void updateEditableRejectsMissingOption() {
        SystemOptionMapper mapper = mock(SystemOptionMapper.class);
        when(mapper.selectById(99L)).thenReturn(null);
        SystemOptionService service = new SystemOptionService(mapper);

        SystemOptionDO patch = new SystemOptionDO();
        patch.setLabel("新标签");

        assertThatThrownBy(() -> service.updateEditable(99L, patch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("系统选项不存在");
    }

    @Test
    void updateEditableOnlyWritesEditableFields() {
        SystemOptionMapper mapper = mock(SystemOptionMapper.class);
        SystemOptionDO existing = new SystemOptionDO();
        existing.setId(1L);
        existing.setCategory("condition_operator");
        existing.setOptionKey("EQ");
        existing.setLabel("等于");
        existing.setEnabled(1);
        existing.setSystemBuiltin(1);
        when(mapper.selectById(1L)).thenReturn(existing);
        SystemOptionService service = new SystemOptionService(mapper);

        SystemOptionDO patch = new SystemOptionDO();
        patch.setCategory("other");
        patch.setOptionKey("BAD");
        patch.setLabel("等于（改）");
        patch.setDescription("描述");
        patch.setSortOrder(90);
        patch.setEnabled(0);

        service.updateEditable(1L, patch);

        verify(mapper).updateById(argThat((SystemOptionDO updated) ->
                updated.getId().equals(1L)
                        && updated.getCategory().equals("condition_operator")
                        && updated.getOptionKey().equals("EQ")
                        && updated.getLabel().equals("等于（改）")
                        && updated.getDescription().equals("描述")
                        && updated.getSortOrder() == 90
                        && updated.getEnabled() == 0));
    }
}
