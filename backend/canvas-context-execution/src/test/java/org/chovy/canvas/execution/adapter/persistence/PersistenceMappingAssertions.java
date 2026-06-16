package org.chovy.canvas.execution.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

final class PersistenceMappingAssertions {

    private PersistenceMappingAssertions() {
    }

    static void assertTable(Class<?> type, String tableName) {
        assertThat(type.getAnnotation(TableName.class))
                .extracting(TableName::value)
                .isEqualTo(tableName);
    }

    static void assertTableId(Class<?> type, String fieldName, IdType idType) {
        TableId tableId = field(type, fieldName).getAnnotation(TableId.class);
        assertThat(tableId).isNotNull();
        assertThat(tableId.type()).isEqualTo(idType);
    }

    static void assertTableId(Class<?> type, String fieldName) {
        assertThat(field(type, fieldName).getAnnotation(TableId.class)).isNotNull();
    }

    static void assertTableField(Class<?> type, String fieldName, String columnName) {
        TableField tableField = field(type, fieldName).getAnnotation(TableField.class);
        assertThat(tableField).isNotNull();
        assertThat(tableField.value()).isEqualTo(columnName);
    }

    private static Field field(Class<?> type, String fieldName) {
        try {
            return type.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("missing field " + fieldName + " on " + type.getName(), e);
        }
    }
}
