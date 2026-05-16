package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.domain.meta.MqMessageDefinition;
import org.chovy.canvas.domain.meta.MqMessageDefinitionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqTriggerHandlerTest {

    @Mock
    MqMessageDefinitionMapper mqMapper;

    @InjectMocks
    MqTriggerHandler handler;

    @Test
    void resolveTopic_from_messageCodeKey() {
        MqMessageDefinition def = new MqMessageDefinition();
        def.setTopic("flight_order_status_change");
        when(mqMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(def);

        String topic = handler.resolveTopic(
            Map.of("messageCodeKey", "flight_order_status_change"));

        assertThat(topic).isEqualTo("flight_order_status_change");
    }

    @Test
    void resolveTopic_falls_back_to_topicKey_for_old_canvases() {
        // No messageCodeKey in config — mapper is never called; fallback to topicKey
        String topic = handler.resolveTopic(
            Map.of("topicKey", "legacy_topic"));

        assertThat(topic).isEqualTo("legacy_topic");
    }
}
