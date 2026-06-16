package org.chovy.canvas.canvas.application;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.chovy.canvas.canvas.api.CanvasCompatibilityFacade;
import org.springframework.stereotype.Service;

/**
 * 封装CanvasCompatibilityApplicationService相关的业务逻辑。
 */
@Service
public class CanvasCompatibilityApplicationService implements CanvasCompatibilityFacade {

    /**
     * 保存内存场景下生成标识或统计次数的原子计数器。
     */
    private final AtomicLong nextCanvasId = new AtomicLong(1000L);

    /**
     * 保存内存场景下生成标识或统计次数的原子计数器。
     */
    private final AtomicLong nextTemplateId = new AtomicLong(5000L);

    /**
     * 保存内存实现使用的canvases by tenant映射数据。
     */
    private final Map<Long, LinkedHashMap<Long, MutableCanvas>> canvasesByTenant = new LinkedHashMap<>();

    /**
     * 保存内存实现使用的templates by tenant映射数据。
     */
    private final Map<Long, LinkedHashMap<Long, MutableTemplate>> templatesByTenant = new LinkedHashMap<>();

    /**
     * 保存内存实现使用的reviews映射数据。
     */
    private final Map<String, ReviewView> reviews = new LinkedHashMap<>();

    /**
     * 创建。
     */
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

    /**
     * 创建Canvas。
     */
    public synchronized Map<String, Object> createCanvas(Long tenantId, Map<String, ?> request, String operator) {
        if (nextCanvasId.get() == 1000L) {
            nextCanvasId.incrementAndGet();
        }
        return toMap(create(tenantId, operator, request));
    }

    /**
     * 获取。
     */
    @Override
    public synchronized CanvasView get(Long tenantId, Long canvasId) {
        return requireCanvas(tenantIdOrDefault(tenantId), canvasId).toView();
    }

    /**
     * 更新。
     */
    @Override
    public synchronized CanvasView update(Long tenantId, String operator, Long canvasId, Map<String, ?> request) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        canvas.update(request, operatorOrDefault(operator));
        return canvas.toView();
    }

    /**
     * 更新兼容接口中的画布基础信息。
     */
    public synchronized Map<String, Object> updateCanvas(Long tenantId,
                                                         Long canvasId,
                                                         Map<String, ?> request,
                                                         String operator) {
        return toMap(update(tenantId, operator, canvasId, request));
    }

    /**
     * 获取Canvas。
     */
    public synchronized Map<String, Object> getCanvas(Long tenantId, Long canvasId) {
        return toMap(get(tenantId, canvasId));
    }

    /**
     * 列出Canvases。
     */
    public synchronized List<Map<String, Object>> listCanvases(Long tenantId) {
        return tenantCanvases(tenantIdOrDefault(tenantId)).values().stream()
                .map(MutableCanvas::toView)
                .sorted((left, right) -> right.id().compareTo(left.id()))
                .map(CanvasCompatibilityApplicationService::toMap)
                .toList();
    }

    /**
     * 列出。
     */
    @Override
    public synchronized PageView<CanvasView> list(Long tenantId) {
        List<CanvasView> list = tenantCanvases(tenantIdOrDefault(tenantId)).values().stream()
                .map(MutableCanvas::toView)
                .toList();
        return new PageView<>(list.size(), list);
    }

    /**
     * 处理submitReview。
     */
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

    /**
     * 提交画布审核并记录审核信息。
     */
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

    /**
     * 处理approvalStatusView。
     */
    @Override
    public synchronized ApprovalStatusView approvalStatusView(Long tenantId, Long canvasId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        ReviewView review = reviews.get(reviewKey(canvas.tenantId, canvas.id));
        String status = review == null ? "NOT_SUBMITTED" : review.status();
        return new ApprovalStatusView(canvas.id, status, review == null ? null : review.reviewId());
    }

    /**
     * 处理approvalStatus。
     */
    public synchronized Map<String, Object> approvalStatus(Long tenantId, Long canvasId) {
        ApprovalStatusView status = approvalStatusView(tenantId, canvasId);
        return mapOf("canvasId", status.canvasId(), "status", status.status(), "reviewId", status.reviewId());
    }

    /**
     * 处理prePublishChecksView。
     */
    @Override
    public synchronized PrePublishCheckView prePublishChecksView(Long tenantId, Long canvasId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        boolean graphPresent = canvas.graphJson != null && !canvas.graphJson.isBlank();
        return new PrePublishCheckView(graphPresent, List.of(
                new CheckItemView("GRAPH_PRESENT", graphPresent, graphPresent ? "Graph is present" : "Graph is empty"),
                new CheckItemView("CANVAS_STATUS", true, "Canvas can be checked")));
    }

    /**
     * 处理prePublishChecks。
     */
    public synchronized Map<String, Object> prePublishChecks(Long tenantId, Long canvasId) {
        PrePublishCheckView checks = prePublishChecksView(tenantId, canvasId);
        return mapOf(
                "passed", checks.passed(),
                "blockerCount", checks.passed() ? 0 : 1,
                "items", checks.items());
    }

    /**
     * 处理revert。
     */
    @Override
    public synchronized OperationView revert(Long tenantId, String operator, Long canvasId, Long versionId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        canvas.graphJson = canvas.versions.getOrDefault(versionId, canvas.graphJson);
        canvas.activeVersionId = versionId;
        canvas.updatedBy = operatorOrDefault(operator);
        canvas.editVersion += 1;
        return new OperationView(canvas.id, "REVERTED", versionId, canvas.updatedBy);
    }

    /**
     * 处理revert。
     */
    public synchronized Map<String, Object> revert(Long tenantId, Long canvasId, Long versionId, String operator) {
        revert(tenantId, operator, canvasId, versionId);
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        return toMap(canvas.toView());
    }

    /**
     * 处理startCanary。
     */
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

    /**
     * 处理startCanary。
     */
    public synchronized Map<String, Object> startCanary(Long tenantId, Long canvasId, int percent, String operator) {
        return toMap(startCanary(tenantId, operator, canvasId, percent));
    }

    /**
     * 处理promoteCanary。
     */
    @Override
    public synchronized CanvasView promoteCanary(Long tenantId, String operator, Long canvasId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        canvas.status = "PUBLISHED";
        canvas.canaryStatus = "PROMOTED";
        canvas.updatedBy = operatorOrDefault(operator);
        canvas.editVersion += 1;
        return canvas.toView();
    }

    /**
     * 处理promoteCanary。
     */
    public synchronized Map<String, Object> promoteCanary(Long tenantId, Long canvasId, String operator) {
        return toMap(promoteCanary(tenantId, operator, canvasId));
    }

    /**
     * 处理rollbackCanary。
     */
    @Override
    public synchronized CanvasView rollbackCanary(Long tenantId, String operator, Long canvasId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        canvas.canaryPercent = 0;
        canvas.canaryStatus = "ROLLED_BACK";
        canvas.updatedBy = operatorOrDefault(operator);
        canvas.editVersion += 1;
        return canvas.toView();
    }

    /**
     * 处理rollbackCanary。
     */
    public synchronized Map<String, Object> rollbackCanary(Long tenantId, Long canvasId, String operator) {
        return toMap(rollbackCanary(tenantId, operator, canvasId));
    }

    /**
     * 处理rollback。
     */
    @Override
    public synchronized CanvasView rollback(Long tenantId, String operator, Long canvasId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        canvas.status = "ROLLED_BACK";
        canvas.updatedBy = operatorOrDefault(operator);
        canvas.editVersion += 1;
        return canvas.toView();
    }

    /**
     * 处理rollback。
     */
    public synchronized Map<String, Object> rollback(Long tenantId, Long canvasId, String operator) {
        return toMap(rollback(tenantId, operator, canvasId));
    }

    /**
     * 处理cloneCanvas。
     */
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

    /**
     * 处理cloneCanvas。
     */
    public synchronized Map<String, Object> cloneCanvas(Long tenantId, Long canvasId, String operator) {
        return toMap(cloneCanvas(tenantId, operator, canvasId));
    }

    /**
     * 处理diff。
     */
    @Override
    public synchronized DiffView diff(Long tenantId, Long canvasId, Long leftVersionId, Long rightVersionId) {
        requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        return new DiffView(true, List.of(new DiffItemView(
                "VERSION_CHANGED",
                "version",
                String.valueOf(leftVersionId),
                String.valueOf(rightVersionId))));
    }

    /**
     * 比较两个版本之间的配置差异。
     */
    public synchronized Map<String, Object> diffVersions(Long tenantId,
                                                         Long canvasId,
                                                         Long leftVersionId,
                                                         Long rightVersionId) {
        DiffView diff = diff(tenantId, canvasId, leftVersionId, rightVersionId);
        return mapOf("canvasId", canvasId, "changed", diff.changed(), "changes", diff.changes());
    }

    /**
     * 处理safeUpdate。
     */
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

    /**
     * 在安全校验通过后更新画布。
     */
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

    /**
     * 处理previewMessage。
     */
    @Override
    public synchronized MessagePreviewView previewMessage(Long tenantId, Long canvasId, Map<String, ?> request) {
        requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        String nodeId = stringValue(request, "nodeId", "message");
        return new MessagePreviewView(canvasId, nodeId, stringValue(request, "userId", ""), "Preview for " + nodeId);
    }

    /**
     * 处理messagePreview。
     */
    public synchronized Map<String, Object> messagePreview(Long tenantId, Long canvasId, Map<String, ?> request) {
        MessagePreviewView preview = previewMessage(tenantId, canvasId, request);
        return mapOf(
                "canvasId", preview.canvasId(),
                "nodeId", preview.nodeId(),
                "userId", preview.userId(),
                "previewText", preview.previewText(),
                "rendered", true);
    }

    /**
     * 处理exportCanvasView。
     */
    @Override
    public synchronized CanvasExportView exportCanvasView(Long tenantId, Long canvasId, Long versionId) {
        MutableCanvas canvas = requireCanvas(tenantIdOrDefault(tenantId), canvasId);
        String packageJson = "{\"canvasId\":" + canvas.id + ",\"versionId\":" + versionId + ",\"name\":\""
                + escapeJson(canvas.name) + "\"}";
        return new CanvasExportView(canvas.id, versionId, packageJson);
    }

    /**
     * 处理exportCanvas。
     */
    public synchronized Map<String, Object> exportCanvas(Long tenantId, Long canvasId, Long versionId) {
        CanvasExportView export = exportCanvasView(tenantId, canvasId, versionId);
        return mapOf("canvasId", export.canvasId(), "versionId", export.versionId(), "packageJson", export.packageJson());
    }

    /**
     * 处理importCanvas。
     */
    @Override
    public synchronized CanvasImportView importCanvas(Long tenantId, Map<String, ?> request) {
        String operator = stringValue(request, "operator", "operator-1");
        CanvasView created = create(tenantIdOrDefault(tenantId), operator, Map.of(
                "name", stringValue(request, "name", "Imported Canvas"),
                "description", "Imported from package",
                "graphJson", "{}"));
        return new CanvasImportView(created, stringValue(request, "packageJson", "{}"));
    }

    /**
     * 处理importCanvas。
     */
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

    /**
     * 列出Templates。
     */
    public synchronized List<TemplateView> listTemplates(Long tenantId, String category) {
        return tenantTemplates(tenantIdOrDefault(tenantId)).values().stream()
                .filter(template -> template.enabled)
                .filter(template -> category == null || category.isBlank() || category.equals(template.category))
                .sorted((left, right) -> Integer.compare(right.useCount, left.useCount))
                .map(MutableTemplate::toView)
                .toList();
    }

    /**
     * 保存AsTemplate。
     */
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

    /**
     * 根据模板创建新的画布视图。
     */
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

    /**
     * 处理pendingReviews。
     */
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

    /**
     * 批量执行画布操作并汇总结果。
     */
    public synchronized BatchOperationView batchOperation(Long tenantId,
                                                          String operator,
                                                          String operation,
                                                          Map<String, ?> request) {
        Long normalizedTenantId = tenantIdOrDefault(tenantId);
        String normalizedOperation = normalizeOperation(operation);
        // 先固定操作类型，再逐项执行，确保批量结果中的状态口径一致。
        List<BatchItemView> results = resolveBatchCanvasIds(normalizedTenantId, request).stream()
                .map(canvasId -> runBatchItem(normalizedTenantId, operatorOrDefault(operator), normalizedOperation,
                        canvasId, request))
                .toList();
        return new BatchOperationView(normalizedOperation, results);
    }

    /**
     * 执行单个批量操作项。
     */
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

    /**
     * 处理pauseBatchItem。
     */
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

    /**
     * 处理resumeBatchItem。
     */
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

    /**
     * 处理archiveBatchItem。
     */
    private BatchItemView archiveBatchItem(MutableCanvas canvas, String operator) {
        if ("ARCHIVED".equals(canvas.status)) {
            return new BatchItemView(canvas.id, null, "SKIPPED", "ALREADY_ARCHIVED");
        }
        canvas.status = "ARCHIVED";
        canvas.updatedBy = operator;
        canvas.editVersion += 1;
        return new BatchItemView(canvas.id, null, "SUCCESS", "ARCHIVED");
    }

    /**
     * 处理cloneBatchItem。
     */
    private BatchItemView cloneBatchItem(MutableCanvas canvas, String operator, Map<String, ?> request) {
        CanvasView clone = cloneCanvas(canvas.tenantId, operator, canvas.id);
        MutableCanvas mutableClone = requireCanvas(canvas.tenantId, clone.id());
        applyBatchCloneReplacements(mutableClone, mapValue(request, "replacements"));
        return new BatchItemView(canvas.id, mutableClone.id, "SUCCESS", "CLONED");
    }

    /**
     * 校验并返回Canvas。
     */
    private MutableCanvas requireCanvas(Long tenantId, Long canvasId) {
        MutableCanvas canvas = tenantCanvases(tenantId).get(canvasId);
        if (canvas == null) {
            throw new IllegalArgumentException("画布不存在: " + canvasId);
        }
        return canvas;
    }

    /**
     * 处理tenantCanvases。
     */
    private LinkedHashMap<Long, MutableCanvas> tenantCanvases(Long tenantId) {
        return canvasesByTenant.computeIfAbsent(tenantId, ignored -> new LinkedHashMap<>());
    }

    /**
     * 处理tenantTemplates。
     */
    private LinkedHashMap<Long, MutableTemplate> tenantTemplates(Long tenantId) {
        return templatesByTenant.computeIfAbsent(tenantId, ignored -> new LinkedHashMap<>());
    }

    /**
     * 处理resolveBatchCanvasIds。
     */
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

    /**
     * 处理longListValue。
     */
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

    /**
     * 处理mapValue。
     */
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

    /**
     * 规范化Operation。
     */
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

    /**
     * 处理applyBatchCloneReplacements。
     */
    private static void applyBatchCloneReplacements(MutableCanvas clone, Map<String, ?> replacements) {
        String name = stringValue(replacements, "name", clone.name);
        String description = stringValue(replacements, "description", clone.description);
        clone.name = replaceTokens(name, replacements);
        clone.description = replaceTokens(description, replacements);
    }

    /**
     * 处理replaceTokens。
     */
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

    /**
     * 处理tenantIdOrDefault。
     */
    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? 7L : tenantId;
    }

    /**
     * 处理operatorOrDefault。
     */
    private static String operatorOrDefault(String operator) {
        return operator == null || operator.isBlank() ? "operator-1" : operator;
    }

    /**
     * 处理reviewKey。
     */
    private static String reviewKey(Long tenantId, Long canvasId) {
        return tenantId + ":" + canvasId;
    }

    /**
     * 处理stringValue。
     */
    private static String stringValue(Map<String, ?> request, String key, String defaultValue) {
        if (request == null) {
            return defaultValue;
        }
        Object value = request.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    /**
     * 处理intValue。
     */
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

    /**
     * 处理escapeJSON 内容。
     */
    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 转换为Map。
     */
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

    /**
     * 处理mapOf。
     */
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

    /**
     * 封装MutableCanvas相关的业务逻辑。
     */
    private static final class MutableCanvas {

        /**
         * 保存标识。
         */
        private final Long id;

        /**
         * 保存租户标识。
         */
        private final Long tenantId;

        /**
         * 保存创建人。
         */
        private final String createdBy;

        /**
         * 保存名称。
         */
        private String name;

        /**
         * 保存描述。
         */
        private String description;

        /**
         * 保存graphJSON 内容。
         */
        private String graphJson;

        /**
         * 保存状态。
         */
        private String status;

        /**
         * 保存更新人。
         */
        private String updatedBy;

        /**
         * 保存editVersion。
         */
        private int editVersion;

        /**
         * 保存canaryPercent。
         */
        private int canaryPercent;

        /**
         * 保存canaryStatus。
         */
        private String canaryStatus;

        /**
         * 保存source canvas标识。
         */
        private Long sourceCanvasId;

        /**
         * 保存imported。
         */
        private boolean imported;

        /**
         * 保存active version标识。
         */
        private Long activeVersionId = 1L;

        /**
         * 保存内存实现使用的versions映射数据。
         */
        private final Map<Long, String> versions = new LinkedHashMap<>();

        /**
         * 创建可变画布内存行。
         */
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

        /**
         * 更新。
         */
        private void update(Map<String, ?> request, String operator) {
            this.name = stringValue(request, "name", name);
            this.description = stringValue(request, "description", description);
            this.graphJson = stringValue(request, "graphJson", graphJson);
            this.updatedBy = operator;
            this.editVersion += 1;
            this.activeVersionId += 1;
            this.versions.put(activeVersionId, graphJson);
        }

        /**
         * 转换为View。
         */
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

    /**
     * 封装MutableTemplate相关的业务逻辑。
     */
    private static final class MutableTemplate {

        /**
         * 保存标识。
         */
        private final Long id;

        /**
         * 保存租户标识。
         */
        private final Long tenantId;

        /**
         * 保存名称。
         */
        private final String name;

        /**
         * 保存描述。
         */
        private final String description;

        /**
         * 保存category。
         */
        private final String category;

        /**
         * 保存graphJSON 内容。
         */
        private final String graphJson;

        /**
         * 保存official。
         */
        private final boolean official;

        /**
         * 保存启用状态。
         */
        private final boolean enabled;

        /**
         * 保存创建人。
         */
        private final String createdBy;

        /**
         * 保存useCount。
         */
        private int useCount;

        /**
         * 创建可变模板内存行。
         */
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

        /**
         * 转换为View。
         */
        private TemplateView toView() {
            return new TemplateView(id, tenantId, name, description, category, graphJson, official, enabled,
                    useCount, createdBy);
        }
    }

    /**
     * 承载CanvasView的数据快照。
     */
    public record CanvasView(
            /**
             * 记录标识。
             */
            Long id,
            /**
             * 记录租户标识。
             */
            Long tenantId,
            /**
             * 记录名称。
             */
            String name,
            /**
             * 记录描述。
             */
            String description,
            /**
             * 记录graphJSON 内容。
             */
            String graphJson,
            /**
             * 记录状态。
             */
            String status,
            /**
             * 记录创建人。
             */
            String createdBy,
            /**
             * 记录更新人。
             */
            String updatedBy,
            /**
             * 记录editVersion。
             */
            Integer editVersion,
            /**
             * 记录canaryPercent。
             */
            Integer canaryPercent,
            /**
             * 记录canaryStatus。
             */
            String canaryStatus,
            /**
             * 记录source canvas标识。
             */
            Long sourceCanvasId,
            /**
             * 记录active version标识。
             */
            Long activeVersionId,
            /**
             * 记录imported。
             */
            Boolean imported) {
    }

    /**
     * 承载PageView的数据快照。
     */
    public record PageView<T>(int total, List<T> list) {
    }

    /**
     * 承载TemplateView的数据快照。
     */
    public record TemplateView(
            /**
             * 记录标识。
             */
            Long id,
            /**
             * 记录租户标识。
             */
            Long tenantId,
            /**
             * 记录名称。
             */
            String name,
            /**
             * 记录描述。
             */
            String description,
            /**
             * 记录category。
             */
            String category,
            /**
             * 记录graphJSON 内容。
             */
            String graphJson,
            /**
             * 记录official。
             */
            Boolean official,
            /**
             * 记录启用状态。
             */
            Boolean enabled,
            /**
             * 记录useCount。
             */
            Integer useCount,
            /**
             * 记录创建人。
             */
            String createdBy) {
    }

    /**
     * 承载ReviewView的数据快照。
     */
    public record ReviewView(String reviewId, Long canvasId, String status, String operator, String reason) {
    }

    /**
     * 承载PendingReviewView的数据快照。
     */
    public record PendingReviewView(String reviewId, Long canvasId, String status, String operator, String reason) {
    }

    /**
     * 承载ApprovalStatusView的数据快照。
     */
    public record ApprovalStatusView(Long canvasId, String status, String reviewId) {
    }

    /**
     * 承载PrePublishCheckView的数据快照。
     */
    public record PrePublishCheckView(boolean passed, List<CheckItemView> items) {
    }

    /**
     * 承载CheckItemView的数据快照。
     */
    public record CheckItemView(String code, boolean passed, String message) {
    }

    /**
     * 承载OperationView的数据快照。
     */
    public record OperationView(Long canvasId, String status, Long versionId, String operator) {
    }

    /**
     * 承载DiffView的数据快照。
     */
    public record DiffView(boolean changed, List<DiffItemView> changes) {
    }

    /**
     * 承载DiffItemView的数据快照。
     */
    public record DiffItemView(String code, String path, String before, String after) {
    }

    /**
     * 承载MessagePreviewView的数据快照。
     */
    public record MessagePreviewView(Long canvasId, String nodeId, String userId, String previewText) {
    }

    /**
     * 承载CanvasExportView的数据快照。
     */
    public record CanvasExportView(Long canvasId, Long versionId, String packageJson) {
    }

    /**
     * 承载CanvasImportView的数据快照。
     */
    public record CanvasImportView(CanvasView created, String packageJson) {
    }

    /**
     * 承载BatchItemView的数据快照。
     */
    public record BatchItemView(Long canvasId, Long targetCanvasId, String status, String message) {

        /**
         * 处理new canvas标识。
         */
        public Long newCanvasId() {
            return targetCanvasId;
        }
    }

    /**
     * 承载BatchOperationView的数据快照。
     */
    public record BatchOperationView(String operation,
                                     /**
                                      * 记录totalCount。
                                      */
                                     int totalCount,
                                     /**
                                      * 记录successCount。
                                      */
                                     int successCount,
                                     /**
                                      * 记录skippedCount。
                                      */
                                     int skippedCount,
                                     /**
                                      * 记录failedCount。
                                      */
                                     int failedCount,
                                     /**
                                      * 记录items。
                                      */
                                     List<BatchItemView> items,
                                     /**
                                      * 记录countsByStatus。
                                      */
                                     Map<String, Integer> countsByStatus) {

        private BatchOperationView(String operation, List<BatchItemView> items) {
            this(operation, items.size(), count(items, "SUCCESS"), count(items, "SKIPPED"), count(items, "FAILED"),
                    items, countsByStatus(items));
        }

        /**
         * 保存测试或内存实现使用的results列表。
         */
        public List<BatchItemView> results() {
            return items;
        }

        /**
         * 统计指定状态的批量操作结果数量。
         */
        private static int count(List<BatchItemView> items, String status) {
            return (int) items.stream().filter(item -> status.equals(item.status())).count();
        }

        /**
         * 保存内存实现使用的counts by status映射数据。
         */
        private static Map<String, Integer> countsByStatus(List<BatchItemView> items) {
            LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
            counts.put("SUCCESS", count(items, "SUCCESS"));
            counts.put("SKIPPED", count(items, "SKIPPED"));
            counts.put("FAILED", count(items, "FAILED"));
            return counts;
        }
    }
}
