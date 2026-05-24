package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import org.chovy.canvas.dal.dataobject.TagDefinitionDO;

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
