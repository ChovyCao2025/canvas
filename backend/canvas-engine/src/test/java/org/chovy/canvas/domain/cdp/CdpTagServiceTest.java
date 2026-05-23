package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.chovy.canvas.domain.meta.TagDefinition;
import org.chovy.canvas.domain.meta.TagDefinitionMapper;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpTagServiceTest {

    private TagDefinitionMapper tagDefinitionMapper;
    private CdpUserTagMapper userTagMapper;
    private CdpUserTagHistoryMapper historyMapper;
    private CdpUserService userService;
    private CdpTagService service;

    @BeforeEach
    void setUp() {
        tagDefinitionMapper = Mockito.mock(TagDefinitionMapper.class);
        userTagMapper = Mockito.mock(CdpUserTagMapper.class);
        historyMapper = Mockito.mock(CdpUserTagHistoryMapper.class);
        userService = Mockito.mock(CdpUserService.class);
        service = new CdpTagService(tagDefinitionMapper, userTagMapper, historyMapper, userService);
    }

    @Test
    void setTagUpsertsCurrentTagAndWritesHistory() {
        when(tagDefinitionMapper.selectOne(any(Wrapper.class))).thenReturn(tag("vip", "BOOLEAN", 1, 1));
        when(userTagMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        CdpTagWriteReq req = new CdpTagWriteReq("vip", "true", "manual mark",
                null, "MANUAL", "req-1", "admin", null);
        service.setTag("u1", req);

        ArgumentCaptor<CdpUserTag> tagCaptor = ArgumentCaptor.forClass(CdpUserTag.class);
        ArgumentCaptor<CdpUserTagHistory> historyCaptor = ArgumentCaptor.forClass(CdpUserTagHistory.class);
        verify(userTagMapper).insert(tagCaptor.capture());
        verify(historyMapper).insert(historyCaptor.capture());

        assertThat(tagCaptor.getValue().getUserId()).isEqualTo("u1");
        assertThat(tagCaptor.getValue().getTagCode()).isEqualTo("vip");
        assertThat(tagCaptor.getValue().getTagValue()).isEqualTo("true");
        assertThat(tagCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(historyCaptor.getValue().getOperation()).isEqualTo("SET");
        assertThat(historyCaptor.getValue().getNewValue()).isEqualTo("true");
    }

    @Test
    void setTagRejectsBooleanValueMismatch() {
        when(tagDefinitionMapper.selectOne(any(Wrapper.class))).thenReturn(tag("vip", "BOOLEAN", 1, 1));
        CdpTagWriteReq req = new CdpTagWriteReq("vip", "yes", "bad value",
                null, "MANUAL", "req-1", "admin", null);

        assertThatThrownBy(() -> service.setTag("u1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BOOLEAN");
    }

    @Test
    void removeTagMarksCurrentTagRemovedAndWritesHistory() {
        CdpUserTag existing = new CdpUserTag();
        existing.setUserId("u1");
        existing.setTagCode("vip");
        existing.setTagValue("true");
        existing.setStatus("ACTIVE");
        when(userTagMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        service.removeTag("u1", "vip", "cleanup", "admin");

        assertThat(existing.getStatus()).isEqualTo("REMOVED");
        verify(userTagMapper).updateById(existing);
        verify(historyMapper).insert(any(CdpUserTagHistory.class));
    }

    private TagDefinition tag(String code, String valueType, int enabled, int manualEnabled) {
        TagDefinition def = new TagDefinition();
        def.setTagCode(code);
        def.setName(code);
        def.setEnabled(enabled);
        def.setValueType(valueType);
        def.setManualEnabled(manualEnabled);
        def.setDefaultTtlDays(null);
        return def;
    }
}
