package org.chovy.canvas.domain.compliance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import org.chovy.canvas.dal.dataobject.CanvasAuditLogDO;
import org.chovy.canvas.dal.mapper.CanvasAuditLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * AuditEventService 编排 domain.compliance 场景的领域业务规则。
 */
@Service
public class AuditEventService {

    private final CanvasAuditLogMapper auditLogMapper;
    private final PiiMaskingService maskingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建 AuditEventService 实例并注入 domain.compliance 场景依赖。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param maskingService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public AuditEventService(CanvasAuditLogMapper auditLogMapper, PiiMaskingService maskingService) {
        this.auditLogMapper = auditLogMapper;
        this.maskingService = maskingService;
    }

    /**
     * 记录一条画布相关审计事件。
     * 方法会把租户、目标、请求 ID 和元数据写入 detail JSON，并在写入前对元数据做 PII/密钥脱敏。
     */
    public void record(AuditEventCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        CanvasAuditLogDO row = new CanvasAuditLogDO();
        row.setCanvasId(resolveCanvasId(command));
        row.setOperator(command.getActor());
        row.setOperatorRole(command.getActorRole());
        row.setAction(command.getOperation());
        row.setFromVersion(command.getFromVersion());
        row.setToVersion(command.getToVersion());
        row.setDetail(toDetailJson(command));
        row.setIp(command.getIp());
        row.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(row);
    }

    /**
     * 解析业务依赖或上下文值。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 resolve canvas id 计算得到的数量、金额或指标值。
     */
    private Long resolveCanvasId(AuditEventCommand command) {
        if (!"canvas".equalsIgnoreCase(command.getTargetType())) {
            return 0L;
        }
        try {
            return Long.parseLong(command.getTargetId());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回组装或转换后的结果对象。
     */
    private String toDetailJson(AuditEventCommand command) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("tenantId", command.getTenantId());
        detail.put("targetType", command.getTargetType());
        detail.put("targetId", command.getTargetId());
        detail.put("requestId", command.getRequestId());
        detail.put("metadata", maskingService.maskMetadata(command.getMetadata()));
        try {
            return objectMapper.writeValueAsString(detail);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize audit detail", ex);
        }
    }

    @Getter
    @Builder
    /**
     * AuditEventCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public static class AuditEventCommand {
        private final Long tenantId;
        private final String actor;
        private final String actorRole;
        private final String operation;
        private final String targetType;
        private final String targetId;
        private final String requestId;
        private final String ip;
        private final Long fromVersion;
        private final Long toVersion;
        private final Map<String, Object> metadata;
    }
}
