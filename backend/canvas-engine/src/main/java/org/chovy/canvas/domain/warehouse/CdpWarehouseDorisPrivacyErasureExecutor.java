package org.chovy.canvas.domain.warehouse;

/**
 * CdpWarehouseDorisPrivacyErasureExecutor 定义 domain.warehouse 场景中的扩展契约。
 */
public interface CdpWarehouseDorisPrivacyErasureExecutor {

    /**
     * 执行核心业务处理流程。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    Result execute(Command command);

    /**
     * Command 数据记录。
     */
    record Command(
            String assetKey,
            Long tenantId,
            String subjectType,
            String subjectValue,
            boolean dryRun,
            String actor) {
    }

    /**
     * Result 数据记录。
     */
    record Result(
            String status,
            long matchedCount,
            long affectedCount,
            String proofMessage,
            String errorMessage) {
    }
}
