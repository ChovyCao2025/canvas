package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.chovy.canvas.dal.dataobject.TagDefinitionDO;
import org.chovy.canvas.dal.mapper.TagDefinitionMapper;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagHistoryDO;
import org.chovy.canvas.dal.mapper.CdpUserTagHistoryMapper;
import org.chovy.canvas.dal.mapper.CdpUserTagMapper;

/**
 * Cdp Tag 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
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

        ArgumentCaptor<CdpUserTagDO> tagCaptor = ArgumentCaptor.forClass(CdpUserTagDO.class);
        ArgumentCaptor<CdpUserTagHistoryDO> historyCaptor = ArgumentCaptor.forClass(CdpUserTagHistoryDO.class);
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
    void setTagDoesNotMutateCurrentTagWhenIdempotencyKeyAlreadyExists() {
        when(tagDefinitionMapper.selectOne(any(Wrapper.class))).thenReturn(tag("vip", "BOOLEAN", 1, 1));
        when(userTagMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doThrow(new DuplicateKeyException("duplicate idempotency"))
                .when(historyMapper).insert(any(CdpUserTagHistoryDO.class));
        CdpTagWriteReq req = new CdpTagWriteReq("vip", "true", "manual mark",
                null, "MANUAL", "req-1", "admin", "idem-1");

        service.setTag("u1", req);

        verify(userTagMapper, never()).insert(any(CdpUserTagDO.class));
        verify(userTagMapper, never()).updateById(any(CdpUserTagDO.class));
        verify(userService, never()).ensureUser(any(), any(), any());
    }


    @Test
    void removeTagMarksCurrentTagRemovedAndWritesHistory() {
        CdpUserTagDO existing = new CdpUserTagDO();
        existing.setUserId("u1");
        existing.setTagCode("vip");
        existing.setTagValue("true");
        existing.setStatus("ACTIVE");
        when(userTagMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        service.removeTag("u1", "vip", "cleanup", "admin");

        assertThat(existing.getStatus()).isEqualTo("REMOVED");
        verify(userTagMapper).updateById(existing);
        verify(historyMapper).insert(any(CdpUserTagHistoryDO.class));
    }

    private TagDefinitionDO tag(String code, String valueType, int enabled, int manualEnabled) {
        TagDefinitionDO def = new TagDefinitionDO();
        def.setTagCode(code);
        def.setName(code);
        def.setEnabled(enabled);
        def.setValueType(valueType);
        def.setManualEnabled(manualEnabled);
        def.setDefaultTtlDays(null);
        return def;
    }
}
