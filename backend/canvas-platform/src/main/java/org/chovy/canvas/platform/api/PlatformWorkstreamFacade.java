package org.chovy.canvas.platform.api;

import java.util.List;

/**
 * 提供平台工作流状态查询和执行准入校验的应用入口。
 */
public interface PlatformWorkstreamFacade {

    /**
     * 查询全部平台工作流的当前状态。
     *
     * @return 工作流状态视图列表
     */
    List<WorkstreamStatusView> statuses();

    /**
     * 要求指定工作流具备可执行子规格。
     *
     * @param workstreamKey 工作流稳定键
     * @return 满足准入条件的工作流状态视图
     */
    WorkstreamStatusView requireExecutableChildSpec(String workstreamKey);
}
