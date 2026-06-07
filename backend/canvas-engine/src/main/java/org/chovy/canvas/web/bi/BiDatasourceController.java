package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceConnectorCapability;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceApiPreview;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceApiPreviewRequest;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceConnectionTestResult;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceCredentialRotationCommand;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceCredentialRotationView;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceFileMaterializationCommand;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceFileMaterializationResult;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceFileMaterializationService;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceFileUploadCommand;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceFileUploadService;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceOnboardingCommand;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceOnboardingService;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceOnboardingView;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceRuntimeService;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceSchemaPreview;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceSchemaSnapshotView;
import org.chovy.canvas.domain.datasource.DataSourceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas/bi/datasources")
public class BiDatasourceController {

    private final TenantContextResolver tenantContextResolver;
    private final BiDatasourceOnboardingService onboardingService;
    private final BiDatasourceRuntimeService runtimeService;
    private final DataSourceConfigService dataSourceConfigService;
    private final BiDatasourceFileUploadService fileUploadService;
    private final BiDatasourceFileMaterializationService fileMaterializationService;

    public BiDatasourceController(TenantContextResolver tenantContextResolver,
                                  BiDatasourceOnboardingService onboardingService) {
        this(tenantContextResolver, onboardingService, null, null, null, null);
    }

    public BiDatasourceController(TenantContextResolver tenantContextResolver,
                                  BiDatasourceOnboardingService onboardingService,
                                  BiDatasourceRuntimeService runtimeService) {
        this(tenantContextResolver, onboardingService, runtimeService, null, null, null);
    }

    public BiDatasourceController(TenantContextResolver tenantContextResolver,
                                  BiDatasourceOnboardingService onboardingService,
                                  BiDatasourceRuntimeService runtimeService,
                                  DataSourceConfigService dataSourceConfigService) {
        this(tenantContextResolver, onboardingService, runtimeService, dataSourceConfigService, null, null);
    }

    public BiDatasourceController(TenantContextResolver tenantContextResolver,
                                  BiDatasourceOnboardingService onboardingService,
                                  BiDatasourceRuntimeService runtimeService,
                                  DataSourceConfigService dataSourceConfigService,
                                  BiDatasourceFileUploadService fileUploadService) {
        this(tenantContextResolver,
                onboardingService,
                runtimeService,
                dataSourceConfigService,
                fileUploadService,
                null);
    }

    @Autowired
    public BiDatasourceController(TenantContextResolver tenantContextResolver,
                                  BiDatasourceOnboardingService onboardingService,
                                  BiDatasourceRuntimeService runtimeService,
                                  DataSourceConfigService dataSourceConfigService,
                                  BiDatasourceFileUploadService fileUploadService,
                                  BiDatasourceFileMaterializationService fileMaterializationService) {
        this.tenantContextResolver = tenantContextResolver;
        this.onboardingService = onboardingService;
        this.runtimeService = runtimeService;
        this.dataSourceConfigService = dataSourceConfigService;
        this.fileUploadService = fileUploadService;
        this.fileMaterializationService = fileMaterializationService;
    }

    @GetMapping("/connectors")
    public Mono<R<List<BiDatasourceConnectorCapability>>> connectorCatalog() {
        return Mono.fromCallable(() -> R.ok(onboardingService.connectorCatalog()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/onboarding")
    public Mono<R<List<BiDatasourceOnboardingView>>> listOnboardingSources() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(onboardingService.listOnboardingSources(normalizeTenant(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/onboarding")
    public Mono<R<BiDatasourceOnboardingView>> createDatasource(@RequestBody BiDatasourceOnboardingCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(onboardingService.createOnboardingSource(
                                normalizeTenant(context),
                                normalizeUsername(context),
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping(value = "/file-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<R<BiDatasourceOnboardingView>> uploadFileDatasource(
            @RequestPart("file") FilePart file,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String sheetName,
            @RequestParam(defaultValue = ",") String delimiter,
            @RequestParam(defaultValue = "true") boolean headerRow,
            @RequestParam(defaultValue = "UTF-8") String encoding) {
        return currentTenant().flatMap(context -> DataBufferUtils.join(file.content())
                .flatMap(buffer -> {
                    byte[] bytes;
                    try {
                        bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                    return Mono.fromCallable(() -> R.ok(requireFileUploadService().upload(
                                    normalizeTenant(context),
                                    normalizeUsername(context),
                                    file.filename(),
                                    bytes,
                                    new BiDatasourceFileUploadCommand(
                                            name,
                                            description,
                                            sheetName,
                                            delimiter,
                                            headerRow,
                                            encoding))))
                            .subscribeOn(Schedulers.boundedElastic());
                }));
    }

    @PostMapping(value = "/file-upload/materialize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<R<BiDatasourceFileMaterializationResult>> uploadAndMaterializeFileDatasource(
            @RequestPart("file") FilePart file,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String sheetName,
            @RequestParam(defaultValue = ",") String delimiter,
            @RequestParam(defaultValue = "true") boolean headerRow,
            @RequestParam(defaultValue = "UTF-8") String encoding,
            @RequestParam(required = false) String datasetKey,
            @RequestParam(required = false) String datasetName,
            @RequestParam(defaultValue = "tenant_id") String tenantColumn,
            @RequestParam(defaultValue = "200") int schemaLimit,
            @RequestParam(defaultValue = "100000") long maxRows) {
        return currentTenant().flatMap(context -> DataBufferUtils.join(file.content())
                .flatMap(buffer -> {
                    byte[] bytes;
                    try {
                        bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                    return Mono.fromCallable(() -> R.ok(requireFileMaterializationService().uploadAndMaterialize(
                                    normalizeTenant(context),
                                    normalizeUsername(context),
                                    normalizeRole(context),
                                    file.filename(),
                                    bytes,
                                    new BiDatasourceFileMaterializationCommand(
                                            name,
                                            description,
                                            sheetName,
                                            delimiter,
                                            headerRow,
                                            encoding,
                                            datasetKey,
                                            datasetName,
                                            tenantColumn,
                                            schemaLimit,
                                            maxRows))))
                            .subscribeOn(Schedulers.boundedElastic());
                }));
    }

    @PutMapping("/onboarding/{id}")
    public Mono<R<BiDatasourceOnboardingView>> updateDatasource(@PathVariable Long id,
                                                                @RequestBody BiDatasourceOnboardingCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(onboardingService.updateOnboardingSource(
                                normalizeTenant(context),
                                normalizeUsername(context),
                                id,
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/connection-test")
    public Mono<R<BiDatasourceConnectionTestResult>> testConnection(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeService().testConnection(id, normalizeTenant(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/credential-rotation")
    public Mono<R<BiDatasourceCredentialRotationView>> rotateCredential(
            @PathVariable Long id,
            @RequestBody BiDatasourceCredentialRotationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireDataSourceConfigService().rotatePassword(
                            id,
                            command == null ? null : command.password(),
                            context);
                    return R.ok(new BiDatasourceCredentialRotationView(
                            id,
                            "jdbc-" + id,
                            normalizeUsername(context)));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}/schema-preview")
    public Mono<R<BiDatasourceSchemaPreview>> schemaPreview(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "100") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeService().previewSchema(id, normalizeTenant(context), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/api-preview")
    public Mono<R<BiDatasourceApiPreview>> apiPreview(@PathVariable Long id,
                                                      @RequestBody(required = false) BiDatasourceApiPreviewRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeService().previewApiData(id, normalizeTenant(context), request)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/schema-sync")
    public Mono<R<BiDatasourceSchemaSnapshotView>> syncSchema(@PathVariable Long id,
                                                              @RequestParam(defaultValue = "100") int limit,
                                                              @RequestBody(required = false) BiDatasourceApiPreviewRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeService().syncSchema(
                                id,
                                normalizeTenant(context),
                                normalizeUsername(context),
                                limit,
                                request)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}/schema-snapshot")
    public Mono<R<BiDatasourceSchemaSnapshotView>> latestSchemaSnapshot(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeService().latestSchemaSnapshot(id, normalizeTenant(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}/schema-snapshots")
    public Mono<R<List<BiDatasourceSchemaSnapshotView>>> schemaSnapshotHistory(@PathVariable Long id,
                                                                               @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeService().schemaSnapshotHistory(id, normalizeTenant(context), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    private Long normalizeTenant(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    private String normalizeUsername(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }

    private String normalizeRole(TenantContext context) {
        return context == null ? null : context.role();
    }

    private BiDatasourceRuntimeService requireRuntimeService() {
        if (runtimeService == null) {
            throw new IllegalStateException("BI datasource runtime service is not configured");
        }
        return runtimeService;
    }

    private DataSourceConfigService requireDataSourceConfigService() {
        if (dataSourceConfigService == null) {
            throw new IllegalStateException("data source config service is not configured");
        }
        return dataSourceConfigService;
    }

    private BiDatasourceFileUploadService requireFileUploadService() {
        if (fileUploadService == null) {
            throw new IllegalStateException("BI datasource file upload service is not configured");
        }
        return fileUploadService;
    }

    private BiDatasourceFileMaterializationService requireFileMaterializationService() {
        if (fileMaterializationService == null) {
            throw new IllegalStateException("BI datasource file materialization service is not configured");
        }
        return fileMaterializationService;
    }
}
