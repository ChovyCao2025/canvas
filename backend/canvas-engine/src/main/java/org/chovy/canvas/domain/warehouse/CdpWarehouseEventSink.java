package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpEventLogDO;

/**
 * CdpWarehouseEventSink 定义 domain.warehouse 场景中的扩展契约。
 */
public interface CdpWarehouseEventSink {

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param event event 参数，用于 writeAccepted 流程中的校验、计算或对象转换。
     */
    void writeAccepted(CdpEventLogDO event);
}
