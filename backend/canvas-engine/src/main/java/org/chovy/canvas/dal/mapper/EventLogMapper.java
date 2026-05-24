package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.EventLogDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事件上报日志 Mapper（表：event_log）。
 *
 * <p>记录外部事件上报请求与处理结果，便于审计和排查。
 */
@Mapper
public interface EventLogMapper extends BaseMapper<EventLogDO> {
    // 日志清理与归档由定时任务负责，Mapper 仅提供持久化能力。
    // 排查事件漏触发问题时，通常先查询本表请求日志。
    // 若需要链路追踪，可结合 execution_id/msg_id 做跨表关联。
}
