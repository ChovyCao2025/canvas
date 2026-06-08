package org.chovy.canvas.domain.bi.storage;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * LocalBiFileStorage 编排 domain.bi.storage 场景的领域业务规则。
 */
public class LocalBiFileStorage implements BiFileStorage {

    public static final String PROVIDER = "LOCAL";

    private final Path root;

    /**
     * 创建 LocalBiFileStorage 实例并注入 domain.bi.storage 场景依赖。
     * @param root root 参数，用于 LocalBiFileStorage 流程中的校验、计算或对象转换。
     */
    public LocalBiFileStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    /**
     * 返回本地磁盘存储提供方标识。
     *
     * @return 固定值 {@code LOCAL}，用于导出和附件记录识别存储后端
     */
    @Override
    public String provider() {
        return PROVIDER;
    }

    /**
     * 将字节数组写入租户隔离后的本地存储路径。
     *
     * @param storageKey 业务生成的相对存储 key，不能为空且不能越过根目录
     * @param bytes 文件内容；为空时写入空文件
     * @return 存储对象描述，包含 provider、逻辑 key、本地路径和文件大小
     */
    @Override
    public BiStoredFile write(String storageKey, byte[] bytes) {
        Path file = resolve(storageKey);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, bytes == null ? new byte[0] : bytes);
            return new BiStoredFile(PROVIDER, storageKey, file.toString(), Files.size(file));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException e) {
            throw new IllegalStateException("failed to write BI storage object: " + storageKey, e);
        }
    }

    /**
     * 通过流式写入器生成本地 BI 文件。
     *
     * @param storageKey 业务生成的相对存储 key，不能为空且不能越过根目录
     * @param writer 文件内容写入器；为空时创建空文件
     * @return 存储对象描述，包含 provider、逻辑 key、本地路径和文件大小
     */
    @Override
    public BiStoredFile write(String storageKey, BiFileStorageWriter writer) {
        Path file = resolve(storageKey);
        try {
            Files.createDirectories(file.getParent());
            try (var output = Files.newOutputStream(file)) {
                if (writer != null) {
                    writer.write(output);
                }
            }
            return new BiStoredFile(PROVIDER, storageKey, file.toString(), Files.size(file));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException e) {
            throw new IllegalStateException("failed to write BI storage object: " + storageKey, e);
        }
    }

    /**
     * 读取本地 BI 文件内容。
     *
     * @param storageKey 业务生成的相对存储 key
     * @return 文件完整字节内容
     */
    @Override
    public byte[] read(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException e) {
            throw new IllegalStateException("BI storage object is not available: " + storageKey, e);
        }
    }

    /**
     * 删除本地 BI 文件并清理空父目录。
     *
     * @param storageKey 业务生成的相对存储 key
     * @return {@code true} 表示文件存在且已删除，{@code false} 表示目标不存在
     */
    @Override
    public boolean delete(String storageKey) {
        Path file = resolve(storageKey);
        try {
            boolean deleted = Files.deleteIfExists(file);
            deleteEmptyParents(file.getParent());
            return deleted;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException e) {
            throw new IllegalStateException("failed to delete BI storage object: " + storageKey, e);
        }
    }

    /**
     * 将逻辑存储 key 解析为本地绝对路径，并阻止路径穿越。
     */
    private Path resolve(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey is required");
        }
        Path file = root.resolve(storageKey).toAbsolutePath().normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("storageKey escapes BI storage root: " + storageKey);
        }
        return file;
    }

    /**
     * 自底向上删除空目录，直到存储根目录或遇到非空目录为止。
     */
    private void deleteEmptyParents(Path parent) throws IOException {
        Path cursor = parent;
        while (cursor != null && cursor.startsWith(root) && !cursor.equals(root)) {
            try {
                Files.deleteIfExists(cursor);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (DirectoryNotEmptyException e) {
                return;
            }
            cursor = cursor.getParent();
        }
    }
}
