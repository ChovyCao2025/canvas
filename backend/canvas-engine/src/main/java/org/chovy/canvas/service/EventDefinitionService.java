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

    /**
     * 执行 do Report Event 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param req 请求对象，承载调用方提交的业务参数
     * @return 按业务键组织的映射结果
     */
    Map<String, Object> doReportEvent(EventReportReq req);
}
