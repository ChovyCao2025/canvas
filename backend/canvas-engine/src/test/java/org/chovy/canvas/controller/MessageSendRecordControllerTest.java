package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.chovy.canvas.web.MessageSendRecordController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageSendRecordControllerTest {

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                MessageSendRecordDO.class);
    }

    @Test
    void messageSendRecordPrimaryKeyIsMappedToIdColumn() {
        assertThat(TableInfoHelper.getTableInfo(MessageSendRecordDO.class).getKeyColumn())
                .isEqualTo("id");
    }

    @Test
    void listFiltersByCanvasUserChannelAndStatus() {
        MessageSendRecordMapper mapper = mock(MessageSendRecordMapper.class);
        MessageSendRecordController controller = new MessageSendRecordController(mapper);
        Page<MessageSendRecordDO> page = new Page<>(1, 20);
        page.setTotal(0);
        when(mapper.selectPage(any(), any())).thenReturn(page);

        controller.list(10L, null, "user-1", "sms", "sent", null, null, 1, 20).block();

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<LambdaQueryWrapper<MessageSendRecordDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertThat(sql).contains("canvas_id").contains("user_id").contains("channel").contains("status");
        assertThat(captor.getValue().getParamNameValuePairs().values())
                .contains(10L, "user-1", "SMS", "SENT");
    }

    @Test
    void detailReturnsNotFoundMessageWhenRecordIsMissing() {
        MessageSendRecordMapper mapper = mock(MessageSendRecordMapper.class);
        MessageSendRecordController controller = new MessageSendRecordController(mapper);
        when(mapper.selectById(99L)).thenReturn(null);

        R<MessageSendRecordDO> response = controller.detail(99L).block();

        assertThat(response.getCode()).isEqualTo(-1);
        assertThat(response.getMessage()).isEqualTo("发送记录不存在: 99");
    }
}
