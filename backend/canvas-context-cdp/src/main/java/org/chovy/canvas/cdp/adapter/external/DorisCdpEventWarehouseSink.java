package org.chovy.canvas.cdp.adapter.external;

import org.chovy.canvas.cdp.domain.CdpEventLog;
import org.chovy.canvas.cdp.domain.CdpWarehouseEventSinkPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * 表示 DorisCdpEventWarehouseSink 的业务数据或处理组件。
 */
@Component
@ConditionalOnBean(DorisCdpEventWarehouseSink.DorisCdpEventWriter.class)
public class DorisCdpEventWarehouseSink implements CdpWarehouseEventSinkPort {

    /**
     * writer。
     */
    private final DorisCdpEventWriter writer;

    /**
     * 创建当前组件实例。
     */
    public DorisCdpEventWarehouseSink(DorisCdpEventWriter writer) {
        this.writer = writer;
    }

    /**
     * 执行 mirrorAccepted 对应的 CDP 业务操作。
     */
    @Override
    public void mirrorAccepted(CdpEventLog eventLog) {
        writer.writeAccepted(eventLog);
    }

    /**
     * 定义 DorisCdpEventWriter 的协作契约。
     */
    public interface DorisCdpEventWriter {
        /**
         * 执行 writeAccepted 对应的 CDP 业务操作。
         */
        void writeAccepted(CdpEventLog eventLog);
    }
}
