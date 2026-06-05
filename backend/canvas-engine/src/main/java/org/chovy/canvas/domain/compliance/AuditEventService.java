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

@Service
public class AuditEventService {

    private final CanvasAuditLogMapper auditLogMapper;
    private final PiiMaskingService maskingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditEventService(CanvasAuditLogMapper auditLogMapper, PiiMaskingService maskingService) {
        this.auditLogMapper = auditLogMapper;
        this.maskingService = maskingService;
    }

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

    private Long resolveCanvasId(AuditEventCommand command) {
        if (!"canvas".equalsIgnoreCase(command.getTargetType())) {
            return 0L;
        }
        try {
            return Long.parseLong(command.getTargetId());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String toDetailJson(AuditEventCommand command) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("tenantId", command.getTenantId());
        detail.put("targetType", command.getTargetType());
        detail.put("targetId", command.getTargetId());
        detail.put("requestId", command.getRequestId());
        detail.put("metadata", maskingService.maskMetadata(command.getMetadata()));
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize audit detail", ex);
        }
    }

    @Getter
    @Builder
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
