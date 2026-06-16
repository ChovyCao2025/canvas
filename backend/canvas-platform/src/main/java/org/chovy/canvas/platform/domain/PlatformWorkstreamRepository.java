package org.chovy.canvas.platform.domain;

import java.util.List;

/**
 * 读取平台工作流定义的仓储接口。
 */
public interface PlatformWorkstreamRepository {

    /**
     * 查询所有平台工作流定义。
     *
     * @return 平台工作流列表
     */
    List<PlatformWorkstream> list();

    /**
     * 按稳定键查询单个工作流定义。
     *
     * @param workstreamKey 工作流稳定键
     * @return 匹配的工作流定义
     */
    PlatformWorkstream get(String workstreamKey);
}
