package org.chovy.canvas.domain.bi.storage;

import java.io.IOException;
import java.io.OutputStream;

/**
 * BiFileStorageWriter 定义 domain.bi.storage 场景中的扩展契约。
 */
@FunctionalInterface
public interface BiFileStorageWriter {

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param output output 参数，用于 write 流程中的校验、计算或对象转换。
     */
    void write(OutputStream output) throws IOException;
}
