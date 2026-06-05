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

    public MqMessageDefinitionService(MqMessageDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    public MqMessageDefinitionDO findEnabledByMessageCode(String messageCode) {
        if (messageCode == null || messageCode.isBlank()) {
            return null;
        }
        return mapper.selectOne(
                new LambdaQueryWrapper<MqMessageDefinitionDO>()
                        .eq(MqMessageDefinitionDO::getMessageCode, messageCode)
                        .eq(MqMessageDefinitionDO::getEnabled, 1));
    }

    public String resolveTopic(Map<String, Object> config) {
        String messageCode = (String) config.get(MapFieldKeys.MESSAGE_CODE_KEY);
        MqMessageDefinitionDO definition = findEnabledByMessageCode(messageCode);
        if (definition != null) {
            return definition.getTopic();
        }
        return (String) config.getOrDefault(MapFieldKeys.TOPIC_KEY, "");
    }
}
