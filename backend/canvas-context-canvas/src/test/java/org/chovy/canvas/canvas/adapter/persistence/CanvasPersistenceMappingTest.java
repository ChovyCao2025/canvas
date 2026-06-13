package org.chovy.canvas.canvas.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import java.lang.reflect.Field;

import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasRuntimeOptions;
import org.chovy.canvas.canvas.domain.CanvasStatus;
import org.junit.jupiter.api.Test;

class CanvasPersistenceMappingTest {

    @Test
    void mapsCanvasDomainToPersistenceRowAndBack() {
        Canvas canvas = Canvas.createDraft(7L, 9L, "Welcome", "desc", "creator").publish(77L);

        CanvasDO row = CanvasPersistenceMapper.toRow(canvas);
        Canvas restored = CanvasPersistenceMapper.toDomain(row);

        assertThat(row.getId()).isEqualTo(7L);
        assertThat(row.getTenantId()).isEqualTo(9L);
        assertThat(row.getName()).isEqualTo("Welcome");
        assertThat(row.getStatus()).isEqualTo(1);
        assertThat(row.getPublishedVersionId()).isEqualTo(77L);
        assertThat(restored.status()).isEqualTo(CanvasStatus.PUBLISHED);
        assertThat(restored.publishedVersionId()).isEqualTo(77L);
    }

    @Test
    void mapsRuntimePolicyFieldsWithoutForcingNullRuntimeUpdates() {
        Canvas canvas = Canvas.createDraft(7L, 9L, "Welcome", "desc", "creator")
                .withRuntimeOptions(new CanvasRuntimeOptions(
                        "SCHEDULED",
                        "0 0 10 * * ?",
                        "2026-01-01T00:00",
                        "2026-12-31T23:59",
                        1000,
                        3,
                        10,
                        60,
                        15,
                        "salt-a",
                        "ORDER_PAID",
                        14,
                        "LAST_TOUCH"));

        CanvasDO row = CanvasPersistenceMapper.toRow(canvas);
        Canvas restored = CanvasPersistenceMapper.toDomain(row);

        assertThat(row.getTriggerType()).isEqualTo("SCHEDULED");
        assertThat(row.getCronExpression()).isEqualTo("0 0 10 * * ?");
        assertThat(row.getMaxTotalExecutions()).isEqualTo(1000);
        assertThat(restored.runtimeOptions().toExecutionOptions())
                .containsEntry("triggerType", "SCHEDULED")
                .containsEntry("maxTotalExecutions", 1000)
                .containsEntry("cooldownSeconds", 60);
        assertThat(fieldStrategy("validStart")).isNotEqualTo(FieldStrategy.ALWAYS);
        assertThat(fieldStrategy("validEnd")).isNotEqualTo(FieldStrategy.ALWAYS);
        assertThat(fieldStrategy("maxTotalExecutions")).isNotEqualTo(FieldStrategy.ALWAYS);
        assertThat(fieldStrategy("perUserDailyLimit")).isNotEqualTo(FieldStrategy.ALWAYS);
        assertThat(fieldStrategy("perUserTotalLimit")).isNotEqualTo(FieldStrategy.ALWAYS);
        assertThat(fieldStrategy("cooldownSeconds")).isNotEqualTo(FieldStrategy.ALWAYS);
    }

    @Test
    void nullableLifecyclePointersUseAlwaysUpdateStrategySoTheyCanBeCleared() {
        assertThat(fieldStrategy("publishedVersionId")).isEqualTo(FieldStrategy.ALWAYS);
        assertThat(fieldStrategy("canaryVersionId")).isEqualTo(FieldStrategy.ALWAYS);
        assertThat(fieldStrategy("canaryPercent")).isEqualTo(FieldStrategy.ALWAYS);
        assertThat(fieldStrategy("previousVersionId")).isEqualTo(FieldStrategy.ALWAYS);
    }

    @Test
    void canvasRepositoryRejectsStaleUpdates() {
        CanvasMapper mapper = mock(CanvasMapper.class);
        when(mapper.updateById(any(CanvasDO.class))).thenReturn(0);
        MybatisCanvasRepository repository = new MybatisCanvasRepository(mapper);

        assertThatThrownBy(() -> repository.save(Canvas.createDraft(7L, 9L, "Welcome", "desc", "creator")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Canvas update affected 0 rows");
    }

    private static FieldStrategy fieldStrategy(String fieldName) {
        try {
            Field field = CanvasDO.class.getDeclaredField(fieldName);
            TableField tableField = field.getAnnotation(TableField.class);
            return tableField == null ? FieldStrategy.DEFAULT : tableField.updateStrategy();
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }
}
