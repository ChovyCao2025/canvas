package org.chovy.canvas.web.marketing;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.marketing.api.TagImportFacade;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/tag-imports")
public class TagImportController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final TagImportFacade facade;

    public TagImportController(TagImportFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/api-push")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> apiPush(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) ApiPushReq req) {
        return envelope(() -> facade.apiPush(tenantIdOrDefault(tenantId), req == null ? List.of() : req.rows()));
    }

    @GetMapping("/excel-template")
    public Mono<ResponseEntity<byte[]>> excelTemplate() {
        return Mono.fromCallable(() -> facade.excelTemplate())
                .subscribeOn(Schedulers.boundedElastic())
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                .filename("tag-import-template.xlsx")
                                .build()
                                .toString())
                        .contentType(XLSX)
                        .contentLength(bytes.length)
                        .body(bytes));
    }

    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<CompatibilityEnvelope<Map<String, Object>>> importExcel(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestPart("file") FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> Mono.fromCallable(() -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    return CompatibilityEnvelope.ok(facade.importExcel(tenantIdOrDefault(tenantId),
                            filePart.filename(), bytes));
                }).subscribeOn(Schedulers.boundedElastic())
                        .doFinally(signalType -> DataBufferUtils.release(dataBuffer)));
    }

    @GetMapping("/batches")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listBatches(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listBatches(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/batches/{id}/errors")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listErrors(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable("id") Long batchId) {
        return envelope(() -> facade.listErrors(tenantIdOrDefault(tenantId), batchId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    public record ApiPushReq(List<Map<String, Object>> rows) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
