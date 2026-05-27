package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import org.chovy.canvas.dal.dataobject.TagDefinitionDO;

/**
 * 标签定义 Cdp Fields 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class TagDefinitionCdpFieldsTest {

    @Test
    void exposes_cdp_metadata_fields_without_changing_table_name() {
        TableName tableName = TagDefinitionDO.class.getAnnotation(TableName.class);
        assertThat(tableName.value()).isEqualTo("tag_definition");

        TagDefinitionDO tag = new TagDefinitionDO();
        tag.setValueType("BOOLEAN");
        tag.setManualEnabled(1);
        tag.setDefaultTtlDays(30);
        tag.setCategory("生命周期");
        tag.setOwner("growth");
        tag.setWritePolicy("UPSERT");

        assertThat(tag.getValueType()).isEqualTo("BOOLEAN");
        assertThat(tag.getManualEnabled()).isEqualTo(1);
        assertThat(tag.getDefaultTtlDays()).isEqualTo(30);
        assertThat(tag.getCategory()).isEqualTo("生命周期");
        assertThat(tag.getOwner()).isEqualTo("growth");
        assertThat(tag.getWritePolicy()).isEqualTo("UPSERT");
    }
}
