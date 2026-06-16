package org.chovy.canvas.execution.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 ExecutionRerunFacade 的执行上下文数据结构或业务契约。
 */
public interface ExecutionRerunFacade {

    /**
     * 执行 rerun 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param operator operator 参数
     * @param admin admin 参数
     * @param canvasId canvasId 参数
     * @param command command 参数
     * @return 处理后的结果
     */
    RerunResult rerun(Long tenantId, String operator, boolean admin, Long canvasId, RerunCommand command);

    /**
     * 执行 audit 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param id id 参数
     * @return 处理后的结果
     */
    AuditRow audit(Long tenantId, Long id);

    /**
     * 执行 audits 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     * @return 处理后的结果
     */
    List<AuditRow> audits(Long tenantId, Long canvasId);

    /**
     * 定义 RerunCommand 的执行上下文数据结构或业务契约。
     * @param mode mode 对应的数据字段
     * @param reason reason 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param testUserId testUserId 对应的数据字段
     * @param originalExecutionId originalExecutionId 对应的数据字段
     * @param inputParams inputParams 对应的数据字段
     * @param graphJson graphJson 对应的数据字段
     */
    record RerunCommand(
            String mode,
            String reason,
            String userId,
            Long testUserId,
            String originalExecutionId,
            Map<String, Object> inputParams,
            Map<String, Object> graphJson) {
    }

    /**
     * 定义 RerunResult 的执行上下文数据结构或业务契约。
     * @param auditId auditId 对应的数据字段
     * @param mode mode 对应的数据字段
     * @param status status 对应的数据字段
     * @param execution execution 对应的数据字段
     */
    record RerunResult(Long auditId, String mode, String status, Map<String, Object> execution) {
    }

    /**
     * 定义 AuditRow 的执行上下文数据结构或业务契约。
     * @param id id 对应的数据字段
     * @param tenantId tenantId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param testUserId testUserId 对应的数据字段
     * @param originalExecutionId originalExecutionId 对应的数据字段
     * @param mode mode 对应的数据字段
     * @param reason reason 对应的数据字段
     * @param operator operator 对应的数据字段
     * @param status status 对应的数据字段
     * @param inputParams inputParams 对应的数据字段
     * @param createdAt createdAt 对应的数据字段
     */
    record AuditRow(
            Long id,
            Long tenantId,
            Long canvasId,
            String userId,
            Long testUserId,
            String originalExecutionId,
            String mode,
            String reason,
            String operator,
            String status,
            Map<String, Object> inputParams,
            String createdAt,
            String updatedAt) {
    }
}
