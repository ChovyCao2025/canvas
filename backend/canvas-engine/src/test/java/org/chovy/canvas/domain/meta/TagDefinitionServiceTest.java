package org.chovy.canvas.domain.meta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagDefinitionServiceTest {

    @Mock
    private TagDefinitionMapper tagDefinitionMapper;

    @Mock
    private TagValueDefinitionMapper tagValueDefinitionMapper;

    @Mock
    private UserTagCurrentMapper userTagCurrentMapper;

    @Test
    void create_appliesDefaults() {
        TagDefinitionService service =
                new TagDefinitionService(tagDefinitionMapper, tagValueDefinitionMapper, userTagCurrentMapper);
        TagDefinition body = new TagDefinition();
        body.setName("Age");
        body.setTagCode("user_age");
        body.setTagType("offline");

        TagDefinition created = service.create(body);

        ArgumentCaptor<TagDefinition> captor = ArgumentCaptor.forClass(TagDefinition.class);
        verify(tagDefinitionMapper).insert(captor.capture());
        TagDefinition inserted = captor.getValue();
        assertThat(inserted.getEnabled()).isEqualTo(1);
        assertThat(inserted.getValueType()).isEqualTo("STRING");
        assertThat(created).isSameAs(body);
    }

    @Test
    void ensureValue_insertsMissingRow_withLabelAndSource() {
        TagDefinition tagDefinition = new TagDefinition();
        tagDefinition.setTagCode("tier");
        tagDefinition.setName("Tier");
        tagDefinition.setTagType("offline");
        tagDefinition.setValueType("STRING");
        tagDefinition.setEnabled(1);
        when(tagDefinitionMapper.selectOne(any())).thenReturn(tagDefinition);
        when(tagValueDefinitionMapper.selectOne(any())).thenReturn(null);

        TagDefinitionService service =
                new TagDefinitionService(tagDefinitionMapper, tagValueDefinitionMapper, userTagCurrentMapper);

        service.ensureValue("tier", "vip", "IMPORT");

        ArgumentCaptor<TagValueDefinition> captor = ArgumentCaptor.forClass(TagValueDefinition.class);
        verify(tagValueDefinitionMapper).insert(captor.capture());
        TagValueDefinition inserted = captor.getValue();
        assertThat(inserted.getTagCode()).isEqualTo("tier");
        assertThat(inserted.getValue()).isEqualTo("vip");
        assertThat(inserted.getLabel()).isEqualTo("vip");
        assertThat(inserted.getEnabled()).isEqualTo(1);
        assertThat(inserted.getSortOrder()).isEqualTo(0);
        assertThat(inserted.getSource()).isEqualTo("IMPORT");
    }

    @Test
    void requireEnabledTagAndValidateValue_rejectsInvalidNumber() {
        TagDefinition tagDefinition = new TagDefinition();
        tagDefinition.setTagCode("score");
        tagDefinition.setName("Score");
        tagDefinition.setTagType("offline");
        tagDefinition.setValueType("NUMBER");
        tagDefinition.setEnabled(1);
        when(tagDefinitionMapper.selectOne(any())).thenReturn(tagDefinition);

        TagDefinitionService service =
                new TagDefinitionService(tagDefinitionMapper, tagValueDefinitionMapper, userTagCurrentMapper);

        assertThatThrownBy(() -> service.requireEnabledTagAndValidateValue("score", "abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NUMBER");
    }

    @Test
    void listValues_returnsMapperResults() {
        TagValueDefinition first = new TagValueDefinition();
        first.setValue("vip");
        TagValueDefinition second = new TagValueDefinition();
        second.setValue("normal");
        when(tagValueDefinitionMapper.selectList(any())).thenReturn(List.of(first, second));

        TagDefinitionService service =
                new TagDefinitionService(tagDefinitionMapper, tagValueDefinitionMapper, userTagCurrentMapper);

        assertThat(service.listValues("tier", 1)).containsExactly(first, second);
    }
}
