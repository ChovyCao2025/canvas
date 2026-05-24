package org.chovy.canvas.web;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.TagImportBatchDO;
import org.chovy.canvas.dal.dataobject.TagImportErrorDO;
import org.chovy.canvas.domain.meta.TagImportService;
import org.chovy.canvas.dto.TagImportPushReq;
import org.chovy.canvas.dto.TagImportRow;
import org.chovy.canvas.dto.TagImportResult;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/canvas/tag-imports")
@RequiredArgsConstructor
public class TagImportController {

    static final int MAX_EXCEL_ROWS = 20_000;
    static final List<String> EXCEL_HEADERS = List.of("idType", "idValue", "tagCode", "tagValue", "tagTime");
    static final DateTimeFormatter TAG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TagImportService tagImportService;

    @PostMapping("/api-push")
    public Mono<R<TagImportResult>> apiPush(@RequestBody TagImportPushReq req) {
        return Mono.fromCallable(() -> tagImportService.importRows("API_PUSH", null, null, req.getRows()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @GetMapping("/excel-template")
    public Mono<ResponseEntity<byte[]>> downloadExcelTemplate() {
        return Mono.fromCallable(TagImportController::createTemplateBytes)
                .subscribeOn(Schedulers.boundedElastic())
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                .filename("tag-import-template.xlsx", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                        .contentType(MediaType.parseMediaType(ExcelUtil.XLSX_CONTENT_TYPE))
                        .contentLength(bytes.length)
                        .body(bytes));
    }

    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<R<TagImportResult>> importExcel(@RequestPart("file") FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> Mono.fromCallable(() -> importExcel(dataBuffer, filePart.filename()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .doFinally(signalType -> DataBufferUtils.release(dataBuffer)))
                .map(R::ok);
    }

    @GetMapping("/batches")
    public Mono<R<List<TagImportBatchDO>>> listBatches() {
        return Mono.fromCallable(tagImportService::listBatches)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @GetMapping("/batches/{id}/errors")
    public Mono<R<List<TagImportErrorDO>>> listErrors(@PathVariable("id") Long batchId) {
        return Mono.fromCallable(() -> tagImportService.listErrors(batchId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    static List<TagImportRow> readRows(byte[] bytes) {
        try (ExcelReader reader = ExcelUtil.getReader(new ByteArrayInputStream(bytes), 0)) {
            reader.setIgnoreEmptyRow(true);
            List<Map<String, Object>> maps = reader.readAll();
            if (maps.size() > MAX_EXCEL_ROWS) {
                throw new IllegalArgumentException("excel row count exceeds 20000");
            }
            return IntStream.range(0, maps.size())
                    .mapToObj(index -> toImportRow(index + 2, maps.get(index)))
                    .toList();
        }
    }

    static TagImportRow toImportRow(int rowNo, Map<String, ?> map) {
        TagImportRow row = new TagImportRow();
        row.setRowNo(rowNo);
        row.setIdType(normalizeValue(map.get("idType")));
        row.setIdValue(normalizeValue(map.get("idValue")));
        row.setTagCode(normalizeValue(map.get("tagCode")));
        row.setTagValue(normalizeValue(map.get("tagValue")));
        row.setTagTime(parseTagTime(map.get("tagTime")));
        return row;
    }

    static String normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private TagImportResult importExcel(DataBuffer dataBuffer, String fileName) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        List<TagImportRow> rows = readRows(bytes);
        return tagImportService.importRows("EXCEL_IMPORT", fileName, null, rows);
    }

    private static byte[] createTemplateBytes() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ExcelWriter writer = ExcelUtil.getWriter(true)) {
            writer.writeHeadRow(EXCEL_HEADERS);
            writer.writeRow(List.of("email", "user@example.com", "tier", "vip", "2026-05-23 10:30:00"));
            writer.flush(outputStream, true);
            return outputStream.toByteArray();
        }
    }

    private static LocalDateTime parseTagTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay();
        }
        if (value instanceof Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        if (value instanceof TemporalAccessor temporalAccessor) {
            return LocalDateTime.from(temporalAccessor);
        }
        String text = normalizeValue(value);
        if (text == null) {
            return null;
        }
        return LocalDateTime.parse(text, TAG_TIME_FORMATTER);
    }
}
