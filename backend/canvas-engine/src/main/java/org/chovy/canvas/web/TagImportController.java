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

/**
 * 标签导入 HTTP 控制器，根路由为 {@code /canvas/tag-imports}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/canvas/tag-imports")
@RequiredArgsConstructor
public class TagImportController {

    /** Excel 导入允许的最大数据行数。 */
    static final int MAX_EXCEL_ROWS = 20_000;
    /** Excel 模板和导入解析使用的固定表头。 */
    static final List<String> EXCEL_HEADERS = List.of("idType", "idValue", "tagCode", "tagValue", "tagTime");
    /** 标签时间字段的标准解析格式。 */
    static final DateTimeFormatter TAG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 标签导入服务，用于处理 API 和 Excel 导入。 */
    private final TagImportService tagImportService;

    /**
     * 处理 api Push 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param req 请求对象，承载调用方提交的业务参数
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PostMapping("/api-push")
    public Mono<R<TagImportResult>> apiPush(@RequestBody TagImportPushReq req) {
        return Mono.fromCallable(() -> tagImportService.importRows("API_PUSH", null, null, req.getRows()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 处理 download Excel Template 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @GetMapping("/excel-template")
    public Mono<ResponseEntity<byte[]>> downloadExcelTemplate() {
        return Mono.fromCallable(TagImportController::createTemplateBytes)
                .subscribeOn(Schedulers.boundedElastic())
                .map(bytes -> ResponseEntity.ok()
                        // 使用 attachment 响应头，避免浏览器直接尝试预览二进制 xlsx。
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                .filename("tag-import-template.xlsx", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                        .contentType(MediaType.parseMediaType(ExcelUtil.XLSX_CONTENT_TYPE))
                        .contentLength(bytes.length)
                        .body(bytes));
    }
    /**
     * 导入 标签导入接口，对应 POST /excel。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用：会导入并落库资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param filePart 上传文件。
     * @return 异步返回统一响应，包含导入 标签导入后的业务数据。
     */
    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<R<TagImportResult>> importExcel(@RequestPart("file") FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> Mono.fromCallable(() -> importExcel(dataBuffer, filePart.filename()))
                        .subscribeOn(Schedulers.boundedElastic())
                        // join 后必须释放 DataBuffer，避免大文件上传时堆外内存泄漏。
                        .doFinally(signalType -> DataBufferUtils.release(dataBuffer)))
                .map(R::ok);
    }

    /**
     * 处理 list Batches 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @GetMapping("/batches")
    public Mono<R<List<TagImportBatchDO>>> listBatches() {
        return Mono.fromCallable(tagImportService::listBatches)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }
    /**
     * 查询标签导入列表接口，对应 GET /batches/{id}/errors。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 主要委托 tagImportService.listErrors 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param batchId 批次 ID。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/batches/{id}/errors")
    public Mono<R<List<TagImportErrorDO>>> listErrors(@PathVariable("id") Long batchId) {
        return Mono.fromCallable(() -> tagImportService.listErrors(batchId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 查询或读取业务数据。
     *
     * @param bytes bytes 参数，用于 readRows 流程中的校验、计算或对象转换。
     * @return 返回 read rows 汇总后的集合、分页或映射视图。
     */
    static List<TagImportRow> readRows(byte[] bytes) {
        try (ExcelReader reader = ExcelUtil.getReader(new ByteArrayInputStream(bytes), 0)) {
            reader.setIgnoreEmptyRow(true);
            List<Map<String, Object>> maps = reader.readAll();
            if (maps.size() > MAX_EXCEL_ROWS) {
                throw new IllegalArgumentException("excel row count exceeds 20000");
            }
            // Excel 首行是表头，业务行号从第 2 行开始，便于错误明细回显给用户。
            return IntStream.range(0, maps.size())
                    .mapToObj(index -> toImportRow(index + 2, maps.get(index)))
                    .toList();
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param rowNo row no 参数，用于 toImportRow 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 toImportRow 流程中的校验、计算或对象转换。
     * @param map map 参数，用于 toImportRow 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    static String normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 执行 importExcel 流程，围绕 import excel 完成校验、计算或结果组装。
     *
     * @param dataBuffer data buffer 参数，用于 importExcel 流程中的校验、计算或对象转换。
     * @param fileName 名称文本，用于展示或唯一性校验。
     * @return 返回 importExcel 流程生成的业务结果。
     */
    private TagImportResult importExcel(DataBuffer dataBuffer, String fileName) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        // 读取到 byte[] 后交给 Hutool ExcelReader，避免阻塞解析发生在 Netty 线程。
        dataBuffer.read(bytes);
        List<TagImportRow> rows = readRows(bytes);
        return tagImportService.importRows("EXCEL_IMPORT", fileName, null, rows);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @return 返回流程执行后的业务结果。
     */
    private static byte[] createTemplateBytes() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ExcelWriter writer = ExcelUtil.getWriter(true)) {
            writer.writeHeadRow(EXCEL_HEADERS);
            writer.writeRow(List.of("email", "user@example.com", "tier", "vip", "2026-05-23 10:30:00"));
            writer.flush(outputStream, true);
            return outputStream.toByteArray();
        }
    }

    /**
     * 解析并校验输入数据。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static LocalDateTime parseTagTime(Object value) {
        if (value == null) {
            return null;
        }
        // Hutool 读取 Excel 日期时可能返回多种 Java 时间类型，逐个兼容后再解析字符串。
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
