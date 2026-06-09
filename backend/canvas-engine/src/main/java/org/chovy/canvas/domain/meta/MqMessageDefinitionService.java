package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO;
import org.chovy.canvas.dal.mapper.MqMessageDefinitionMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Domain boundary for MQ message-definition lookup.
 */
@Service
public class MqMessageDefinitionService {

    private final MqMessageDefinitionMapper mapper;

    /**
     * 创建 MqMessageDefinitionService 实例并注入 domain.meta 场景依赖。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MqMessageDefinitionService(MqMessageDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按消息编码查询启用的 MQ 消息定义。
     * 编码为空时返回 null，调用方可据此回退到节点配置中的 topic。
     */
    public MqMessageDefinitionDO findEnabledByMessageCode(String messageCode) {
        if (messageCode == null || messageCode.isBlank()) {
            return null;
        }
        return mapper.selectOne(
                new LambdaQueryWrapper<MqMessageDefinitionDO>()
                        .eq(MqMessageDefinitionDO::getMessageCode, messageCode)
                        .eq(MqMessageDefinitionDO::getEnabled, 1));
    }

    /**
     * 从节点配置解析实际 MQ topic。
     * 优先使用启用消息定义中的 topic，找不到定义时回退到配置里的 topic 字段。
     */
    public String resolveTopic(Map<String, Object> config) {
        String messageCode = (String) config.get(MapFieldKeys.MESSAGE_CODE_KEY);
        MqMessageDefinitionDO definition = findEnabledByMessageCode(messageCode);
        if (definition != null) {
            return definition.getTopic();
        }
        return (String) config.getOrDefault(MapFieldKeys.TOPIC_KEY, "");
    }
}
