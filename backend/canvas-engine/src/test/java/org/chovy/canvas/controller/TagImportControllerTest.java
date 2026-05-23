package org.chovy.canvas.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import org.chovy.canvas.dto.TagImportRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TagImportControllerTest {

    @Test
    void toImportRow_mapsTemplateColumns() {
        TagImportRow row = TagImportController.toImportRow(2, Map.of(
                "idType", "email",
                "idValue", "user@example.com",
                "tagCode", "tier",
                "tagValue", "vip",
                "tagTime", "2026-05-23 10:30:00"
        ));

        assertThat(row.getRowNo()).isEqualTo(2);
        assertThat(row.getIdType()).isEqualTo("email");
        assertThat(row.getIdValue()).isEqualTo("user@example.com");
        assertThat(row.getTagCode()).isEqualTo("tier");
        assertThat(row.getTagValue()).isEqualTo("vip");
        assertThat(row.getTagTime()).isEqualTo(LocalDateTime.of(2026, 5, 23, 10, 30));
    }

    @Test
    void toImportRow_setsRowNo_andTreatsBlankTagTimeAsNull() {
        TagImportRow row = TagImportController.toImportRow(7, Map.of(
                "idType", "email",
                "idValue", "user@example.com",
                "tagCode", "tier",
                "tagValue", "vip",
                "tagTime", "   "
        ));

        assertThat(row.getRowNo()).isEqualTo(7);
        assertThat(row.getTagTime()).isNull();
    }

    @Test
    void readRows_readsExcelRows_andSetsSheetRowNumbers() {
        byte[] bytes = createWorkbookBytes();

        List<TagImportRow> rows = TagImportController.readRows(bytes);

        assertThat(rows).hasSize(1);
        TagImportRow row = rows.getFirst();
        assertThat(row.getRowNo()).isEqualTo(2);
        assertThat(row.getIdType()).isEqualTo("email");
        assertThat(row.getIdValue()).isEqualTo("user@example.com");
        assertThat(row.getTagCode()).isEqualTo("tier");
        assertThat(row.getTagValue()).isEqualTo("vip");
        assertThat(row.getTagTime()).isEqualTo(LocalDateTime.of(2026, 5, 23, 10, 30));
    }

    private byte[] createWorkbookBytes() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ExcelWriter writer = ExcelUtil.getWriter(true)) {
            writer.writeHeadRow(List.of("idType", "idValue", "tagCode", "tagValue", "tagTime"));
            writer.writeRow(List.of("email", "user@example.com", "tier", "vip", "2026-05-23 10:30:00"));
            writer.flush(outputStream, true);
            return outputStream.toByteArray();
        }
    }
}
