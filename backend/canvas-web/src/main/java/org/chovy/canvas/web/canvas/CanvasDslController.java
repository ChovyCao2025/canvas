package org.chovy.canvas.web.canvas;

import java.util.List;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasDefinitionProvider;
import org.chovy.canvas.canvas.api.dsl.CanvasDslDocument;
import org.chovy.canvas.canvas.application.dsl.CanvasDslMappingService;
import org.chovy.canvas.canvas.application.dsl.CanvasDslValidationResult;
import org.chovy.canvas.canvas.application.dsl.CanvasDslValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/canvas/dsl")
public class CanvasDslController {

    private final CanvasDslValidator validator;
    private final CanvasDslMappingService mapper;
    private final PublishedCanvasDefinitionProvider publishedCanvasDefinitionProvider;

    public CanvasDslController(CanvasDslValidator validator, CanvasDslMappingService mapper) {
        this(validator, mapper, null);
    }

    @Autowired
    public CanvasDslController(CanvasDslValidator validator,
                               CanvasDslMappingService mapper,
                               @Qualifier("canvasQueryApplicationService")
                               PublishedCanvasDefinitionProvider publishedCanvasDefinitionProvider) {
        this.validator = validator;
        this.mapper = mapper;
        this.publishedCanvasDefinitionProvider = publishedCanvasDefinitionProvider;
    }

    @PostMapping("/validate")
    public ValidationResponse validate(@RequestBody ValidateRequest request) {
        CanvasDslValidationResult result = validator.validate(request.document());
        return new ValidationResponse(result.valid(), result.violations());
    }

    @PostMapping("/map")
    public MappingResponse map(@RequestBody MapRequest request) {
        CanvasDslValidationResult validation = validator.validate(request.document());
        if (!validation.valid()) {
            return new MappingResponse(null, null, validation.violations());
        }
        var result = mapper.toGraphJson(request.document());
        return new MappingResponse(result.templateKey(), result.graphJson(), List.of());
    }

    @PostMapping("/import")
    public ImportResponse importDsl(@RequestBody ImportRequest request) {
        CanvasDslValidationResult validation = validator.validate(request.document());
        if (!validation.valid()) {
            return new ImportResponse(false, null, null, validation.violations());
        }
        var result = mapper.toGraphJson(request.document());
        return new ImportResponse(true, result.templateKey(), result.graphJson(), List.of());
    }

    @GetMapping("/export/{canvasId}")
    public ExportResponse exportDsl(@RequestHeader("X-Tenant-Id") Long tenantId,
                                    @PathVariable Long canvasId) {
        if (publishedCanvasDefinitionProvider == null) {
            throw new IllegalStateException("Published canvas export provider is not configured");
        }
        PublishedCanvasDefinition definition = publishedCanvasDefinitionProvider.getPublished(tenantId, canvasId);
        List<CanvasDslValidationResult.Violation> unsupportedSemantics;
        CanvasDslDocument document;
        try {
            unsupportedSemantics = mapper.inspectUnsupportedExportSemantics(definition.graphJson());
            if (!unsupportedSemantics.isEmpty()) {
                return nonExportable(definition, unsupportedSemantics);
            }
            document = mapper.fromGraphJson(definition.graphJson());
        } catch (IllegalArgumentException ex) {
            return nonExportable(definition, List.of(new CanvasDslValidationResult.Violation(
                    "UNSUPPORTED_GRAPH_JSON",
                    "Graph JSON cannot be projected to Canvas DSL v1")));
        }
        CanvasDslValidationResult validation = validator.validate(document);
        if (!validation.valid()) {
            return nonExportable(definition, validation.violations());
        }
        return new ExportResponse(
                true,
                definition.canvasId(),
                definition.versionId(),
                definition.version(),
                document,
                null,
                List.of());
    }

    private static ExportResponse nonExportable(PublishedCanvasDefinition definition,
                                                List<CanvasDslValidationResult.Violation> violations) {
        return new ExportResponse(
                false,
                definition.canvasId(),
                definition.versionId(),
                definition.version(),
                null,
                definition.graphJson(),
                violations);
    }

    @PostMapping("/diff")
    public DiffResponse diff(@RequestBody DiffRequest request) {
        CanvasDslValidationResult sourceValidation = validator.validate(request.source());
        CanvasDslValidationResult targetValidation = validator.validate(request.target());
        CanvasDslMappingService.DiffResult result = mapper.diff(request.source(), request.target());
        return new DiffResponse(
                result.changed(),
                result.changes(),
                sourceValidation.violations(),
                targetValidation.violations());
    }

    public record ValidateRequest(CanvasDslDocument document) {
    }

    public record ValidationResponse(boolean valid, List<CanvasDslValidationResult.Violation> violations) {

        public ValidationResponse {
            violations = List.copyOf(violations == null ? List.of() : violations);
        }
    }

    public record MapRequest(CanvasDslDocument document) {
    }

    public record ImportRequest(CanvasDslDocument document) {
    }

    public record MappingResponse(
            String templateKey,
            String graphJson,
            List<CanvasDslValidationResult.Violation> violations) {

        public MappingResponse {
            violations = List.copyOf(violations == null ? List.of() : violations);
        }
    }

    public record ImportResponse(
            boolean importable,
            String templateKey,
            String graphJson,
            List<CanvasDslValidationResult.Violation> violations) {

        public ImportResponse {
            violations = List.copyOf(violations == null ? List.of() : violations);
        }
    }

    public record ExportResponse(
            boolean exportable,
            Long canvasId,
            Long versionId,
            Integer version,
            CanvasDslDocument document,
            String rawGraphJson,
            List<CanvasDslValidationResult.Violation> violations) {

        public ExportResponse {
            violations = List.copyOf(violations == null ? List.of() : violations);
        }
    }

    public record DiffRequest(CanvasDslDocument source, CanvasDslDocument target) {
    }

    public record DiffResponse(
            boolean changed,
            List<CanvasDslMappingService.DiffChange> changes,
            List<CanvasDslValidationResult.Violation> sourceViolations,
            List<CanvasDslValidationResult.Violation> targetViolations) {

        public DiffResponse {
            changes = List.copyOf(changes == null ? List.of() : changes);
            sourceViolations = List.copyOf(sourceViolations == null ? List.of() : sourceViolations);
            targetViolations = List.copyOf(targetViolations == null ? List.of() : targetViolations);
        }
    }
}
