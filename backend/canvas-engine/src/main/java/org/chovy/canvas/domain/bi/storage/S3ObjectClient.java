package org.chovy.canvas.domain.bi.storage;

/**
 * S3ObjectClient 定义 domain.bi.storage 场景中的扩展契约。
 */
public interface S3ObjectClient {

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param bytes bytes 参数，用于 putObject 流程中的校验、计算或对象转换。
     */
    void putObject(S3ObjectRequest request, byte[] bytes);

    /**
     * 查询或读取业务数据。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 getObject 流程生成的业务结果。
     */
    byte[] getObject(S3ObjectRequest request);

    /**
     * 执行数据写入或状态变更。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 delete object 的布尔判断结果。
     */
    boolean deleteObject(S3ObjectRequest request);

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param lifecycleXml lifecycle xml 参数，用于 putBucketLifecycle 流程中的校验、计算或对象转换。
     */
    default void putBucketLifecycle(S3BucketLifecycleRequest request, String lifecycleXml) {
        throw new UnsupportedOperationException("S3 bucket lifecycle is not supported");
    }
}
