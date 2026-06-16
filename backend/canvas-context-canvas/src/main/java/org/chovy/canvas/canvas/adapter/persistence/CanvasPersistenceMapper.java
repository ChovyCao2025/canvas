package org.chovy.canvas.canvas.adapter.persistence;

import java.time.LocalDateTime;

import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasRuntimeOptions;
import org.chovy.canvas.canvas.domain.CanvasStatus;

/**
 * 封装CanvasPersistenceMapper相关的业务逻辑。
 */
final class CanvasPersistenceMapper {

    /**
     * 创建当前对象实例。
     */
    private CanvasPersistenceMapper() {
    }

    /**
     * 转换为Row。
     */
    static CanvasDO toRow(Canvas canvas) {
        CanvasDO row = new CanvasDO();
        row.setId(canvas.id());
        row.setTenantId(canvas.tenantId());
        row.setName(canvas.name());
        row.setDescription(canvas.description());
        row.setStatus(canvas.status().code());
        row.setPublishedVersionId(canvas.publishedVersionId());
        row.setPreviousVersionId(canvas.previousVersionId());
        row.setCanaryVersionId(canvas.canaryVersionId());
        row.setCanaryPercent(canvas.canaryPercent());
        row.setCreatedBy(canvas.createdBy());
        row.setTriggerType(canvas.runtimeOptions().triggerType());
        row.setCronExpression(canvas.runtimeOptions().cronExpression());
        row.setValidStart(parseDateTime(canvas.runtimeOptions().validStart()));
        row.setValidEnd(parseDateTime(canvas.runtimeOptions().validEnd()));
        row.setMaxTotalExecutions(canvas.runtimeOptions().maxTotalExecutions());
        row.setPerUserDailyLimit(canvas.runtimeOptions().perUserDailyLimit());
        row.setPerUserTotalLimit(canvas.runtimeOptions().perUserTotalLimit());
        row.setCooldownSeconds(canvas.runtimeOptions().cooldownSeconds());
        row.setControlGroupPercent(canvas.runtimeOptions().controlGroupPercent());
        row.setControlGroupSalt(canvas.runtimeOptions().controlGroupSalt());
        row.setConversionEventCode(canvas.runtimeOptions().conversionEventCode());
        row.setAttributionWindowDays(canvas.runtimeOptions().attributionWindowDays());
        row.setAttributionModel(canvas.runtimeOptions().attributionModel());
        return row;
    }

    /**
     * 转换为Domain。
     */
    static Canvas toDomain(CanvasDO row) {
        if (row == null) {
            return null;
        }
        return new Canvas(
                row.getId(),
                row.getTenantId(),
                row.getName(),
                row.getDescription(),
                CanvasStatus.fromCode(row.getStatus()),
                row.getPublishedVersionId(),
                row.getPreviousVersionId(),
                row.getCanaryVersionId(),
                row.getCanaryPercent(),
                row.getCreatedBy(),
                new CanvasRuntimeOptions(
                        row.getTriggerType(),
                        row.getCronExpression(),
                        formatDateTime(row.getValidStart()),
                        formatDateTime(row.getValidEnd()),
                        row.getMaxTotalExecutions(),
                        row.getPerUserDailyLimit(),
                        row.getPerUserTotalLimit(),
                        row.getCooldownSeconds(),
                        row.getControlGroupPercent(),
                        row.getControlGroupSalt(),
                        row.getConversionEventCode(),
                        row.getAttributionWindowDays(),
                        row.getAttributionModel()));
    }

    /**
     * 处理parseDateTime。
     */
    private static LocalDateTime parseDateTime(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }

    /**
     * 处理formatDateTime。
     */
    private static String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
