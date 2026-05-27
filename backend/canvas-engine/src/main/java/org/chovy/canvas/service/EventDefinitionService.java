package org.chovy.canvas.service;

import org.chovy.canvas.dto.EventReportReq;

import java.util.Map;

/**
 * 事件定义服务接口。
 *
 * <p>定义行为事件上报入口，负责把外部事件请求转换为画布触发、事件日志和等待恢复等后端流程。
 * <p>接口层保持稳定契约，具体幂等、路由和持久化细节由实现类完成。
 */
public interface EventDefinitionService {

    Map<String, Object> doReportEvent(EventReportReq req);
}
