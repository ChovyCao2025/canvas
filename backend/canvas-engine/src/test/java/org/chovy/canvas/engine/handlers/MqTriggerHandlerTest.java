package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO;
import org.chovy.canvas.dal.mapper.MqMessageDefinitionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Mq Trigger 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
@ExtendWith(MockitoExtension.class)
class MqTriggerHandlerTest {

    @Mock
    MqMessageDefinitionMapper mqMapper;

    @InjectMocks
    MqTriggerHandler handler;

    @Test
    void resolveTopic_from_messageCodeKey() {
        MqMessageDefinitionDO def = new MqMessageDefinitionDO();
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
