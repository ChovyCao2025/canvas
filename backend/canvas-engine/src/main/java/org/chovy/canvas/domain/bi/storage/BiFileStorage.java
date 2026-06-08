package org.chovy.canvas.domain.bi.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * BiFileStorage 定义 domain.bi.storage 场景中的扩展契约。
 */
public interface BiFileStorage {

    /**
     * 执行 provider 流程，围绕 provider 完成校验、计算或结果组装。
     *
     * @return 返回 provider 生成的文本或业务键。
     */
    String provider();

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param storageKey 业务键，用于在同一租户下定位资源。
     * @param bytes bytes 参数，用于 write 流程中的校验、计算或对象转换。
     * @return 返回 write 流程生成的业务结果。
     */
    BiStoredFile write(String storageKey, byte[] bytes);

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param storageKey 业务键，用于在同一租户下定位资源。
     * @param writer writer 参数，用于 write 流程中的校验、计算或对象转换。
     * @return 返回 write 流程生成的业务结果。
     */
    default BiStoredFile write(String storageKey, BiFileStorageWriter writer) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (writer != null) {
                writer.write(output);
            }
            return write(storageKey, output.toByteArray());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException e) {
            throw new IllegalStateException("failed to write BI storage object: " + storageKey, e);
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param storageKey 业务键，用于在同一租户下定位资源。
     * @return 返回 read 流程生成的业务结果。
     */
    byte[] read(String storageKey);

    /**
     * 执行数据写入或状态变更。
     *
     * @param storageKey 业务键，用于在同一租户下定位资源。
     * @return 返回 delete 的布尔判断结果。
     */
    boolean delete(String storageKey);
}
