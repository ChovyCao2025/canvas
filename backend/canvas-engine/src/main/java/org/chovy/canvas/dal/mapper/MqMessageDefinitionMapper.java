package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MQ 消息定义 Mapper（表：mq_message_definition）。
 *
 * <p>供 MQ 触发/发送节点解析 messageCode 与 topic 映射。
 */
@Mapper
public interface MqMessageDefinitionMapper extends BaseMapper<MqMessageDefinitionDO> {
    // topic 绑定、消费位点等 MQ 运行时逻辑不在此层。
    // MQ_TRIGGER 与 SEND_MQ 节点都可能依赖这里的 messageCode 映射。
    // 消息体 schema 变更应由上游消息平台和节点配置协同演进。
}
