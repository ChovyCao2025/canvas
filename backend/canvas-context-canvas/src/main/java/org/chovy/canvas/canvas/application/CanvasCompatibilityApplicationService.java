package org.chovy.canvas.canvas.application;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.chovy.canvas.canvas.api.CanvasCompatibilityFacade;
import org.springframework.stereotype.Service;

@Service
public class CanvasCompatibilityApplicationService implements CanvasCompatibilityFacade {

    private final AtomicLong nextCanvasId = new AtomicLong(1000L);
    private final AtomicLong nextTemplateId = new AtomicLong(5000L);
    private final Map<Long, LinkedHashMap<Long, MutableCanvas>> canvasesByTenant = new LinkedHashMap<>();
    private final Map<Long, LinkedHashMap<Long, MutableTemplate>> templatesByTenant = new LinkedHashMap<>();
    private final Map<String, ReviewView> reviews = new LinkedHashMap<>();

    @Override
    public synchronized CanvasView create(Long tenantId, String operator, Map<String, ?> request) {
        Long normalizedTenantId = tenantIdOrDefault(tenantId);
        String name = stringValue(request, "name", "Untitled Canvas");
        String description = stringValue(request, "description", "");
        String graphJson = stringValue(request, "graphJson", "{}");
        Long id = nextCanvasId.getAndIncrement();
        MutableCanvas canvas = new MutableCanvas(
                id,
                normalizedTenantId,
                name,
                description,
                graphJson,
                "DRAFT",
                operatorOrDefault(operator),
                operatorOrDefault(operator),
                1,
                0,
                null,
                false);
        canvas.versions.put(1L, graphJson);
        tenantCanvases(normalizedTenantId).put(id, canvas);
        return canvas.toView();
    }

    public synchronized Map<String, Object> createCanvas(Long tenantId, Map<String, ?> request, String operator) {
        if (nextCanvasId.get() == 1000L) {
            nextCanvasId.incrementAndGet();
        }
        return toMap(create(tenantId, operator, request));
    }

    @Override
    public synchronized CanvasView get(Long tenantId, Long canvasId) {
        return requireCanvas(tenantIdOrDefault(tenantId), canvasId).toView();
    }

    @Override
    public synchronized CanvasView update(Long tenantId, String operator, Long canvasId, Map<String, ?> request) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        canvas.update(request, operatorOrDefault(operator));
        return canvas.toView();
    }

    public synchronized Map<String, Object> updateCanvas(Long tenantId,
                                                         Long canvasId,
                                                         Map<String, ?> request,
                                                         String operator) {
        return toMap(update(tenantId, operator, canvasId, request));
    }

    public synchronized Map<String, Object> getCanvas(Long tenantId, Long canvasId) {
        return toMap(get(tenantId, canvasId));
    }

    public synchronized List<Map<String, Object>> listCanvases(Long tenantId) {
        return tenantCanvases(tenantIdOrDefault(tenantId)).values().stream()
                .map(MutableCanvas::toView)
                .sorted((left, right) -> right.id().compareTo(left.id()))
                .map(CanvasCompatibilityApplicationService::toMap)
                .toList();
    }

    @Override
    public synchronized PageView<CanvasView> list(Long tenantId) {
        List<CanvasView> list = tenantCanvases(tenantIdOrDefault(tenantId)).values().stream()
                .map(MutableCanvas::toView)
                .toList();
        return new PageView<>(list.size(), list);
    }

    @Override
    public synchronized ReviewView submitReview(Long tenantId, String operator, Long canvasId, Map<String, ?> request) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        ReviewView review = new ReviewView(
                "review-" + canvas.id,
                canvas.id,
                "SUBMITTED",
                operatorOrDefault(operator),
                stringValue(request, "reason", ""));
        reviews.put(reviewKey(canvas.tenantId, canvas.id), review);
        return review;
    }

    public synchronized Map<String, Object> submitReview(Long tenantId,
                                                         Long canvasId,
                                                         Map<String, ?> request,
                                                         String operator) {
        ReviewView review = submitReview(tenantId, operator, canvasId, request);
        return mapOf(
                "reviewId", review.reviewId(),
                "canvasId", review.canvasId(),
                "status", review.status(),
                "operator", review.operator(),
                "reason", review.reason());
    }

    @Override
    public synchronized ApprovalStatusView approvalStatusView(Long tenantId, Long canvasId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        ReviewView review = reviews.get(reviewKey(canvas.tenantId, canvas.id));
        String status = review == null ? "NOT_SUBMITTED" : review.status();
        return new ApprovalStatusView(canvas.id, status, review == null ? null : review.reviewId());
    }

    public synchronized Map<String, Object> approvalStatus(Long tenantId, Long canvasId) {
        ApprovalStatusView status = approvalStatusView(tenantId, canvasId);
        return mapOf("canvasId", status.canvasId(), "status", status.status(), "reviewId", status.reviewId());
    }

    @Override
    public synchronized PrePublishCheckView prePublishChecksView(Long tenantId, Long canvasId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        boolean graphPresent = canvas.graphJson != null && !canvas.graphJson.isBlank();
        return new PrePublishCheckView(graphPresent, List.of(
                new CheckItemView("GRAPH_PRESENT", graphPresent, graphPresent ? "Graph is present" : "Graph is empty"),
                new CheckItemView("CANVAS_STATUS", true, "Canvas can be checked")));
    }

    public synchronized Map<String, Object> prePublishChecks(Long tenantId, Long canvasId) {
        PrePublishCheckView checks = prePublishChecksView(tenantId, canvasId);
        return mapOf(
                "passed", checks.passed(),
                "blockerCount", checks.passed() ? 0 : 1,
                "items", checks.items());
    }

    @Override
    public synchronized OperationView revert(Long tenantId, String operator, Long canvasId, Long versionId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        canvas.graphJson = canvas.versions.getOrDefault(versionId, canvas.graphJson);
        canvas.activeVersionId = versionId;
        canvas.updatedBy = operatorOrDefault(operator);
        canvas.editVersion += 1;
        return new OperationView(canvas.id, "REVERTED", versionId, canvas.updatedBy);
    }

    public synchronized Map<String, Object> revert(Long tenantId, Long canvasId, Long versionId, String operator) {
        revert(tenantId, operator, canvasId, versionId);
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        return toMap(canvas.toView());
    }

    @Override
    public synchronized CanvasView startCanary(Long tenantId, String operator, Long canvasId, int percent) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent must be between 0 and 100");
        }
        canvas.canaryPercent = percent;
        canvas.canaryStatus = "RUNNING";
        canvas.updatedBy = operatorOrDefault(operator);
        canvas.editVersion += 1;
        return canvas.toView();
    }

    public synchronized Map<String, Object> startCanary(Long tenantId, Long canvasId, int percent, String operator) {
        return toMap(startCanary(tenantId, operator, canvasId, percent));
    }

    @Override
    public synchronized CanvasView promoteCanary(Long tenantId, String operator, Long canvasId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        canvas.status = "PUBLISHED";
        canvas.canaryStatus = "PROMOTED";
        canvas.updatedBy = operatorOrDefault(operator);
        canvas.editVersion += 1;
        return canvas.toView();
    }

    public synchronized Map<String, Object> promoteCanary(Long tenantId, Long canvasId, String operator) {
        return toMap(promoteCanary(tenantId, operator, canvasId));
    }

    @Override
    public synchronized CanvasView rollbackCanary(Long tenantId, String operator, Long canvasId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        canvas.canaryPercent = 0;
        canvas.canaryStatus = "ROLLED_BACK";
        canvas.updatedBy = operatorOrDefault(operator);
        canvas.editVersion += 1;
        return canvas.toView();
    }

    public synchronized Map<String, Object> rollbackCanary(Long tenantId, Long canvasId, String operator) {
        return toMap(rollbackCanary(tenantId, operator, canvasId));
    }

    @Override
    public synchronized CanvasView rollback(Long tenantId, String operator, Long canvasId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        canvas.status = "ROLLED_BACK";
        canvas.updatedBy = operatorOrDefault(operator);
        canvas.editVersion += 1;
        return canvas.toView();
    }

    public synchronized Map<String, Object> rollback(Long tenantId, Long canvasId, String operator) {
        return toMap(rollback(tenantId, operator, canvasId));
    }

    @Override
    public synchronized CanvasView cloneCanvas(Long tenantId, String operator, Long canvasId) {
        MutableCanvas source = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        Long id = nextCanvasId.getAndIncrement();
        MutableCanvas clone = new MutableCanvas(
                id,
                source.tenantId,
                source.name + " copy",
                source.description,
                source.graphJson,
                "DRAFT",
                operatorOrDefault(operator),
                operatorOrDefault(operator),
                1,
                0,
                source.id,
                false);
        clone.versions.putAll(source.versions);
        tenantCanvases(source.tenantId).put(id, clone);
        return clone.toView();
    }

    public synchronized Map<String, Object> cloneCanvas(Long tenantId, Long canvasId, String operator) {
        return toMap(cloneCanvas(tenantId, operator, canvasId));
    }

    @Override
    public synchronized DiffView diff(Long tenantId, Long canvasId, Long leftVersionId, Long rightVersionId) {
        requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        return new DiffView(true, List.of(new DiffItemView(
                "VERSION_CHANGED",
                "version",
                String.valueOf(leftVersionId),
                String.valueOf(rightVersionId))));
    }

    public synchronized Map<String, Object> diffVersions(Long tenantId,
                                                         Long canvasId,
                                                         Long leftVersionId,
                                                         Long rightVersionId) {
        DiffView diff = diff(tenantId, canvasId, leftVersionId, rightVersionId);
        return mapOf("canvasId", canvasId, "changed", diff.changed(), "changes", diff.changes());
    }

    @Override
    public synchronized CanvasView safeUpdate(Long tenantId, String operator, Long canvasId, Map<String, ?> request) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        Integer editVersion = intValue(request, "editVersion", canvas.editVersion);
        if (!Integer.valueOf(canvas.editVersion).equals(editVersion)) {
            throw new IllegalStateException("CANVAS_010");
        }
        canvas.update(request, operatorOrDefault(operator));
        return canvas.toView();
    }

    public synchronized Map<String, Object> safeUpdateCanvas(Long tenantId,
                                                             Long canvasId,
                                                             Map<String, ?> request,
                                                             String operator) {
        try {
            return toMap(safeUpdate(tenantId, operator, canvasId, request));
        } catch (IllegalStateException exception) {
            if ("CANVAS_010".equals(exception.getMessage())) {
                throw new IllegalArgumentException("画布已被他人修改，请刷新后重试", exception);
            }
            throw exception;
        }
    }

    @Override
    public synchronized MessagePreviewView previewMessage(Long tenantId, Long canvasId, Map<String, ?> request) {
        requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        String nodeId = stringValue(request, "nodeId", "message");
        return new MessagePreviewView(canvasId, nodeId, stringValue(request, "userId", ""), "Preview for " + nodeId);
    }

    public synchronized Map<String, Object> messagePreview(Long tenantId, Long canvasId, Map<String, ?> request) {
        MessagePreviewView preview = previewMessage(tenantId, canvasId, request);
        return mapOf(
                "canvasId", preview.canvasId(),
                "nodeId", preview.nodeId(),
                "userId", preview.userId(),
                "previewText", preview.previewText(),
                "rendered", true);
    }

    @Override
    public synchronized CanvasExportView exportCanvasView(Long tenantId, Long canvasId, Long versionId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        String packageJson = "{\"canvasId\":" + canvas.id + ",\"versionId\":" + versionId + ",\"name\":\""
                + escapeJson(canvas.name) + "\"}";
        return new CanvasExportView(canvas.id, versionId, packageJson);
    }

    public synchronized Map<String, Object> exportCanvas(Long tenantId, Long canvasId, Long versionId) {
        CanvasExportView export = exportCanvasView(tenantId, canvasId, versionId);
        return mapOf("canvasId", export.canvasId(), "versionId", export.versionId(), "packageJson", export.packageJson());
    }

    @Override
    public synchronized CanvasImportView importCanvas(Long tenantId, Map<String, ?> request) {
        String operator = stringValue(request, "operator", "operator-1");
        CanvasView created = create(tenantIdOrDefault(tenantId), operator, Map.of(
                "name", stringValue(request, "name", "Imported Canvas"),
                "description", "Imported from package",
                "graphJson", "{}"));
        return new CanvasImportView(created, stringValue(request, "packageJson", "{}"));
    }

    public synchronized Map<String, Object> importCanvas(Long tenantId, Map<String, ?> request, String operator) {
        CanvasView created = create(tenantIdOrDefault(tenantId), operator, request);
        return mapOf(
                "id", created.id(),
                "tenantId", created.tenantId(),
                "name", created.name(),
                "graphJson", created.graphJson(),
                "createdBy", created.createdBy(),
                "imported", true);
    }

    public synchronized List<TemplateView> listTemplates(Long tenantId, String category) {
        return tenantTemplates(tenantIdOrDefault(tenantId)).values().stream()
                .filter(template -> template.enabled)
                .filter(template -> category == null || category.isBlank() || category.equals(template.category))
                .sorted((left, right) -> Integer.compare(right.useCount, left.useCount))
                .map(MutableTemplate::toView)
                .toList();
    }

    public synchronized TemplateView saveAsTemplate(Long tenantId, Long canvasId, Map<String, ?> request) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        Long templateId = nextTemplateId.getAndIncrement();
        MutableTemplate template = new MutableTemplate(
                templateId,
                canvas.tenantId,
                stringValue(request, "name", canvas.name + " 模板"),
                stringValue(request, "description", canvas.description),
                stringValue(request, "category", ""),
                canvas.graphJson,
                false,
                true,
                0,
                stringValue(request, "createdBy", "current_user"));
        tenantTemplates(canvas.tenantId).put(templateId, template);
        return template.toView();
    }

    public synchronized CanvasView createFromTemplate(Long tenantId,
                                                      Long templateId,
                                                      Map<String, ?> request,
                                                      String operator) {
        Long normalizedTenantId = tenantIdOrDefault(tenantId);
        MutableTemplate template = tenantTemplates(normalizedTenantId).get(templateId);
        if (template == null || !template.enabled) {
            throw new IllegalArgumentException("模板不存在");
        }
        CanvasView canvas = create(normalizedTenantId, operatorOrDefault(operator), Map.of(
                "name", stringValue(request, "name", template.name + " (副本)"),
                "description", template.description,
                "graphJson", template.graphJson));
        template.useCount += 1;
        return canvas;
    }

    public synchronized List<PendingReviewView> pendingReviews(Long tenantId) {
        Long normalizedTenantId = tenantIdOrDefault(tenantId);
        return reviews.values().stream()
                .filter(review -> tenantCanvases(normalizedTenantId).containsKey(review.canvasId()))
                .filter(review -> "SUBMITTED".equals(review.status()))
                .map(review -> new PendingReviewView(
                        review.reviewId(),
                        review.canvasId(),
                        review.status(),
                        review.operator(),
                        review.reason()))
                .toList();
    }

    public synchronized BatchOperationView batchOperation(Long tenantId,
                                                          String operator,
                                                          String operation,
                                                          Map<String, ?> request) {
        Long normalizedTenantId = tenantIdOrDefault(tenantId);
        String normalizedOperation = normalizeOperation(operation);
        List<BatchItemView> results = resolveBatchCanvasIds(normalizedTenantId, request).stream()
                .map(canvasId -> runBatchItem(normalizedTenantId, operatorOrDefault(operator), normalizedOperation,
                        canvasId, request))
                .toList();
        return new BatchOperationView(normalizedOperation, results);
    }

    private BatchItemView runBatchItem(Long tenantId,
                                       String operator,
                                       String operation,
                                       Long canvasId,
                                       Map<String, ?> request) {
        try {
            MutableCanvas canvas = requireCanvas(tenantId, canvasId);
            return switch (operation) {
                case "PAUSE" -> pauseBatchItem(canvas, operator);
                case "RESUME" -> resumeBatchItem(canvas, operator);
                case "ARCHIVE" -> archiveBatchItem(canvas, operator);
                case "CLONE" -> cloneBatchItem(canvas, operator, request);
                default -> throw new IllegalArgumentException("unsupported batch operation: " + operation);
            };
        } catch (RuntimeException exception) {
            return new BatchItemView(canvasId, null, "FAILED", exception.getMessage());
        }
    }

    private BatchItemView pauseBatchItem(MutableCanvas canvas, String operator) {
        if ("PAUSED".equals(canvas.status)) {
            return new BatchItemView(canvas.id, null, "SKIPPED", "ALREADY_PAUSED");
        }
        if ("ARCHIVED".equals(canvas.status)) {
            return new BatchItemView(canvas.id, null, "SKIPPED", "ARCHIVED");
        }
        canvas.status = "PAUSED";
        canvas.updatedBy = operator;
        canvas.editVersion += 1;
        return new BatchItemView(canvas.id, null, "SUCCESS", "PAUSED");
    }

    private BatchItemView resumeBatchItem(MutableCanvas canvas, String operator) {
        if ("PUBLISHED".equals(canvas.status)) {
            return new BatchItemView(canvas.id, null, "SKIPPED", "ALREADY_PUBLISHED");
        }
        if ("ARCHIVED".equals(canvas.status)) {
            return new BatchItemView(canvas.id, null, "SKIPPED", "ARCHIVED");
        }
        canvas.status = "PUBLISHED";
        canvas.updatedBy = operator;
        canvas.editVersion += 1;
        return new BatchItemView(canvas.id, null, "SUCCESS", "RESUMED");
    }

    private BatchItemView archiveBatchItem(MutableCanvas canvas, String operator) {
        if ("ARCHIVED".equals(canvas.status)) {
            return new BatchItemView(canvas.id, null, "SKIPPED", "ALREADY_ARCHIVED");
        }
        canvas.status = "ARCHIVED";
        canvas.updatedBy = operator;
        canvas.editVersion += 1;
        return new BatchItemView(canvas.id, null, "SUCCESS", "ARCHIVED");
    }

    private BatchItemView cloneBatchItem(MutableCanvas canvas, String operator, Map<String, ?> request) {
        CanvasView clone = cloneCanvas(canvas.tenantId, operator, canvas.id);
        MutableCanvas mutableClone = requireCanvas(canvas.tenantId, clone.id());
        applyBatchCloneReplacements(mutableClone, mapValue(request, "replacements"));
        return new BatchItemView(canvas.id, mutableClone.id, "SUCCESS", "CLONED");
    }

    private MutableCanvas requireCanvas(Long tenantId, Long canvasId) {
        MutableCanvas canvas = tenantCanvases(tenantId).get(canvasId);
        if (canvas == null) {
            throw new IllegalArgumentException("画布不存在: " + canvasId);
        }
        return canvas;
    }

    private LinkedHashMap<Long, MutableCanvas> tenantCanvases(Long tenantId) {
        return canvasesByTenant.computeIfAbsent(tenantId, ignored -> new LinkedHashMap<>());
    }

    private LinkedHashMap<Long, MutableTemplate> tenantTemplates(Long tenantId) {
        return templatesByTenant.computeIfAbsent(tenantId, ignored -> new LinkedHashMap<>());
    }

    private List<Long> resolveBatchCanvasIds(Long tenantId, Map<String, ?> request) {
        List<Long> explicitCanvasIds = longListValue(request, "canvasIds");
        if (!explicitCanvasIds.isEmpty()) {
            LinkedHashSet<Long> deduped = new LinkedHashSet<>(explicitCanvasIds);
            if (deduped.size() > 200) {
                throw new IllegalArgumentException("batch size exceeds 200");
            }
            return deduped.stream().toList();
        }
        Map<String, ?> filter = mapValue(request, "filter");
        String status = stringValue(filter, "status", "");
        String name = stringValue(filter, "name", "");
        List<Long> canvasIds = tenantCanvases(tenantId).values().stream()
                .filter(canvas -> status.isBlank() || status.equalsIgnoreCase(canvas.status))
                .filter(canvas -> name.isBlank() || canvas.name.contains(name))
                .map(canvas -> canvas.id)
                .limit(100)
                .toList();
        if (canvasIds.isEmpty()) {
            throw new IllegalArgumentException("canvasIds or filters must resolve at least one canvas");
        }
        return canvasIds;
    }

    private static List<Long> longListValue(Map<String, ?> request, String key) {
        if (request == null) {
            return List.of();
        }
        Object value = request.get(key);
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        for (Object item : iterable) {
            if (item instanceof Number number) {
                ids.add(number.longValue());
            } else if (item != null && !String.valueOf(item).isBlank()) {
                ids.add(Long.parseLong(String.valueOf(item)));
            }
        }
        return ids;
    }

    private static Map<String, ?> mapValue(Map<String, ?> request, String key) {
        if (request == null) {
            return Map.of();
        }
        Object value = request.get(key);
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((mapKey, mapValue) -> normalized.put(String.valueOf(mapKey), mapValue));
            return normalized;
        }
        return Map.of();
    }

    private static String normalizeOperation(String operation) {
        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("operation is required");
        }
        String normalized = operation.trim().toUpperCase(Locale.ROOT);
        if (!List.of("PAUSE", "RESUME", "ARCHIVE", "CLONE").contains(normalized)) {
            throw new IllegalArgumentException("unsupported batch operation: " + normalized);
        }
        return normalized;
    }

    private static void applyBatchCloneReplacements(MutableCanvas clone, Map<String, ?> replacements) {
        String name = stringValue(replacements, "name", clone.name);
        String description = stringValue(replacements, "description", clone.description);
        clone.name = replaceTokens(name, replacements);
        clone.description = replaceTokens(description, replacements);
    }

    private static String replaceTokens(String input, Map<String, ?> replacements) {
        if (input == null || replacements == null || replacements.isEmpty()) {
            return input;
        }
        String result = input;
        for (Map.Entry<String, ?> replacement : replacements.entrySet()) {
            Object value = replacement.getValue();
            if (value != null) {
                result = result.replace("${" + replacement.getKey() + "}", String.valueOf(value));
            }
        }
        return result;
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? 7L : tenantId;
    }

    private static String operatorOrDefault(String operator) {
        return operator == null || operator.isBlank() ? "operator-1" : operator;
    }

    private static String reviewKey(Long tenantId, Long canvasId) {
        return tenantId + ":" + canvasId;
    }

    private static String stringValue(Map<String, ?> request, String key, String defaultValue) {
        if (request == null) {
            return defaultValue;
        }
        Object value = request.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static Integer intValue(Map<String, ?> request, String key, Integer defaultValue) {
        if (request == null) {
            return defaultValue;
        }
        Object value = request.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Map<String, Object> toMap(CanvasView canvas) {
        return mapOf(
                "id", canvas.id(),
                "tenantId", canvas.tenantId(),
                "name", canvas.name(),
                "description", canvas.description(),
                "graphJson", canvas.graphJson(),
                "status", canvas.status(),
                "createdBy", canvas.createdBy(),
                "updatedBy", canvas.updatedBy(),
                "editVersion", canvas.editVersion(),
                "canaryPercent", canvas.canaryPercent(),
                "canaryStatus", canvas.canaryStatus(),
                "sourceCanvasId", canvas.sourceCanvasId(),
                "activeVersionId", canvas.activeVersionId(),
                "imported", canvas.imported());
    }

    private static Map<String, Object> mapOf(Object... pairs) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            Object value = pairs[i + 1];
            if (value != null) {
                map.put(String.valueOf(pairs[i]), value);
            }
        }
        return map;
    }

    private static final class MutableCanvas {
        private final Long id;
        private final Long tenantId;
        private final String createdBy;
        private String name;
        private String description;
        private String graphJson;
        private String status;
        private String updatedBy;
        private int editVersion;
        private int canaryPercent;
        private String canaryStatus;
        private Long sourceCanvasId;
        private boolean imported;
        private Long activeVersionId = 1L;
        private final Map<Long, String> versions = new LinkedHashMap<>();

        private MutableCanvas(Long id,
                              Long tenantId,
                              String name,
                              String description,
                              String graphJson,
                              String status,
                              String createdBy,
                              String updatedBy,
                              int editVersion,
                              int canaryPercent,
                              Long sourceCanvasId,
                              boolean imported) {
            this.id = id;
            this.tenantId = tenantId;
            this.name = name;
            this.description = description;
            this.graphJson = graphJson;
            this.status = status;
            this.createdBy = createdBy;
            this.updatedBy = updatedBy;
            this.editVersion = editVersion;
            this.canaryPercent = canaryPercent;
            this.canaryStatus = "NONE";
            this.sourceCanvasId = sourceCanvasId;
            this.imported = imported;
        }

        private void update(Map<String, ?> request, String operator) {
            this.name = stringValue(request, "name", name);
            this.description = stringValue(request, "description", description);
            this.graphJson = stringValue(request, "graphJson", graphJson);
            this.updatedBy = operator;
            this.editVersion += 1;
            this.activeVersionId += 1;
            this.versions.put(activeVersionId, graphJson);
        }

        private CanvasView toView() {
            return new CanvasView(
                    id,
                    tenantId,
                    name,
                    description,
                    graphJson,
                    status,
                    createdBy,
                    updatedBy,
                    editVersion,
                    canaryPercent,
                    canaryStatus,
                    sourceCanvasId,
                    activeVersionId,
                    imported);
        }
    }

    private static final class MutableTemplate {
        private final Long id;
        private final Long tenantId;
        private final String name;
        private final String description;
        private final String category;
        private final String graphJson;
        private final boolean official;
        private final boolean enabled;
        private final String createdBy;
        private int useCount;

        private MutableTemplate(Long id,
                                Long tenantId,
                                String name,
                                String description,
                                String category,
                                String graphJson,
                                boolean official,
                                boolean enabled,
                                int useCount,
                                String createdBy) {
            this.id = id;
            this.tenantId = tenantId;
            this.name = name;
            this.description = description;
            this.category = category;
            this.graphJson = graphJson;
            this.official = official;
            this.enabled = enabled;
            this.useCount = useCount;
            this.createdBy = createdBy;
        }

        private TemplateView toView() {
            return new TemplateView(id, tenantId, name, description, category, graphJson, official, enabled,
                    useCount, createdBy);
        }
    }

    public record CanvasView(
            Long id,
            Long tenantId,
            String name,
            String description,
            String graphJson,
            String status,
            String createdBy,
            String updatedBy,
            Integer editVersion,
            Integer canaryPercent,
            String canaryStatus,
            Long sourceCanvasId,
            Long activeVersionId,
            Boolean imported) {
    }

    public record PageView<T>(int total, List<T> list) {
    }

    public record TemplateView(
            Long id,
            Long tenantId,
            String name,
            String description,
            String category,
            String graphJson,
            Boolean official,
            Boolean enabled,
            Integer useCount,
            String createdBy) {
    }

    public record ReviewView(String reviewId, Long canvasId, String status, String operator, String reason) {
    }

    public record PendingReviewView(String reviewId, Long canvasId, String status, String operator, String reason) {
    }

    public record ApprovalStatusView(Long canvasId, String status, String reviewId) {
    }

    public record PrePublishCheckView(boolean passed, List<CheckItemView> items) {
    }

    public record CheckItemView(String code, boolean passed, String message) {
    }

    public record OperationView(Long canvasId, String status, Long versionId, String operator) {
    }

    public record DiffView(boolean changed, List<DiffItemView> changes) {
    }

    public record DiffItemView(String code, String path, String before, String after) {
    }

    public record MessagePreviewView(Long canvasId, String nodeId, String userId, String previewText) {
    }

    public record CanvasExportView(Long canvasId, Long versionId, String packageJson) {
    }

    public record CanvasImportView(CanvasView created, String packageJson) {
    }

    public record BatchItemView(Long canvasId, Long targetCanvasId, String status, String message) {
        public Long newCanvasId() {
            return targetCanvasId;
        }
    }

    public record BatchOperationView(String operation,
                                     int totalCount,
                                     int successCount,
                                     int skippedCount,
                                     int failedCount,
                                     List<BatchItemView> items,
                                     Map<String, Integer> countsByStatus) {

        private BatchOperationView(String operation, List<BatchItemView> items) {
            this(operation, items.size(), count(items, "SUCCESS"), count(items, "SKIPPED"), count(items, "FAILED"),
                    items, countsByStatus(items));
        }

        public List<BatchItemView> results() {
            return items;
        }

        private static int count(List<BatchItemView> items, String status) {
            return (int) items.stream().filter(item -> status.equals(item.status())).count();
        }

        private static Map<String, Integer> countsByStatus(List<BatchItemView> items) {
            LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
            counts.put("SUCCESS", count(items, "SUCCESS"));
            counts.put("SKIPPED", count(items, "SKIPPED"));
            counts.put("FAILED", count(items, "FAILED"));
            return counts;
        }
    }
}
