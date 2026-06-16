package org.chovy.canvas.bi.domain;

import org.chovy.canvas.bi.api.BiPublishApprovalCommand;
import org.chovy.canvas.bi.api.BiPublishApprovalReviewCommand;
import org.chovy.canvas.bi.api.BiPublishApprovalView;
import org.chovy.canvas.bi.api.BiResourceCommentCommand;
import org.chovy.canvas.bi.api.BiResourceCommentView;
import org.chovy.canvas.bi.api.BiResourceLocationCommand;
import org.chovy.canvas.bi.api.BiResourceLocationView;
import org.chovy.canvas.bi.api.BiResourceLockCommand;
import org.chovy.canvas.bi.api.BiResourceLockView;
import org.chovy.canvas.bi.api.BiResourceMoveCommand;
import org.chovy.canvas.bi.api.BiResourceOwnershipView;
import org.chovy.canvas.bi.api.BiResourceTransferCommand;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/**
 * BiResourceOperationsCatalog 目录服务。
 */
public class BiResourceOperationsCatalog {
    /**
     * WORKSPACE_ID 对应的标识。
     */
    private static final Long WORKSPACE_ID = 5L;

    /**
     * comments 对应的数据集合。
     */
    private final Map<Long, BiResourceCommentView> comments = new LinkedHashMap<>();

    /**
     * locks 对应的数据集合。
     */
    private final Map<ResourceRef, BiResourceLockView> locks = new LinkedHashMap<>();

    /**
     * locations 对应的数据集合。
     */
    private final Map<ResourceRef, BiResourceLocationView> locations = new LinkedHashMap<>();

    /**
     * ownerships 对应的数据集合。
     */
    private final Map<ResourceRef, BiResourceOwnershipView> ownerships = new LinkedHashMap<>();

    /**
     * approvals 对应的数据集合。
     */
    private final Map<Long, BiPublishApprovalView> approvals = new LinkedHashMap<>();

    /**
     * nextCommentId 对应的标识。
     */
    private long nextCommentId = 1L;

    /**
     * nextLockId 对应的标识。
     */
    private long nextLockId = 1L;

    /**
     * nextLocationId 对应的标识。
     */
    private long nextLocationId = 1L;

    /**
     * nextOwnershipId 对应的标识。
     */
    private long nextOwnershipId = 1L;

    /**
     * nextApprovalId 对应的标识。
     */
    private long nextApprovalId = 1L;

    /**
     * 执行 add Comment 相关处理。
     */
    public synchronized BiResourceCommentView addComment(Long tenantId,
                                                         BiResourceCommentCommand command,
                                                         String actor,
                                                         LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("BI resource comment command is required");
        }
        ResourceRef ref = ref(tenantId, command.resourceType(), command.resourceKey());
        BiResourceCommentView view = new BiResourceCommentView(
                nextCommentId++,
                ref.tenantId(),
                WORKSPACE_ID,
                ref.resourceType(),
                ref.resourceKey(),
                optionalKey(command.widgetKey()),
                requiredText(command.commentText(), "commentText"),
                actor(actor),
                now,
                null);
        comments.put(view.id(), view);
        return view;
    }
    /**
     * 查询列表数据。
     */
    public synchronized List<BiResourceCommentView> listComments(Long tenantId,
                                                                 String resourceType,
                                                                 String resourceKey) {
        Long scopedTenantId = tenant(tenantId);
        String type = nullableType(resourceType);
        String key = nullableKey(resourceKey);
        return comments.values().stream()
                .filter(comment -> scopedTenantId.equals(comment.tenantId()))
                .filter(comment -> comment.deletedAt() == null)
                .filter(comment -> type == null || type.equals(comment.resourceType()))
                .filter(comment -> key == null || key.equals(comment.resourceKey()))
                .sorted(Comparator.comparing(BiResourceCommentView::id))
                .toList();
    }
    /**
     * 删除业务数据。
     */
    public synchronized void deleteComment(Long tenantId, Long commentId, LocalDateTime now) {
        if (commentId == null || commentId <= 0) {
            return;
        }
        BiResourceCommentView existing = comments.get(commentId);
        if (existing == null || !tenant(tenantId).equals(existing.tenantId()) || existing.deletedAt() != null) {
            return;
        }
        // 评论删除保留原记录并写入 deletedAt，列表查询再按 deletedAt 过滤。
        comments.put(commentId, new BiResourceCommentView(
                existing.id(),
                existing.tenantId(),
                existing.workspaceId(),
                existing.resourceType(),
                existing.resourceKey(),
                existing.widgetKey(),
                existing.commentText(),
                existing.createdBy(),
                existing.createdAt(),
                now));
    }
    /**
     * 执行 acquire Lock 相关处理。
     */
    public synchronized BiResourceLockView acquireLock(Long tenantId,
                                                       BiResourceLockCommand command,
                                                       String actor,
                                                       LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("BI resource lock command is required");
        }
        ResourceRef ref = ref(tenantId, command.resourceType(), command.resourceKey());
        BiResourceLockView existing = locks.get(ref);
        // 同一资源只保留一把锁，重复加锁会刷新令牌、持有人和过期时间。
        BiResourceLockView view = new BiResourceLockView(
                existing == null ? nextLockId++ : existing.id(),
                ref.tenantId(),
                WORKSPACE_ID,
                ref.resourceType(),
                ref.resourceKey(),
                requiredText(command.lockToken(), "lockToken"),
                actor(actor),
                now,
                now.plusSeconds(ttl(command.ttlSeconds())),
                true);
        locks.put(ref, view);
        return view;
    }
    /**
     * 执行 current Lock 相关处理。
     */
    public synchronized BiResourceLockView currentLock(Long tenantId, String resourceType, String resourceKey) {
        ResourceRef ref = ref(tenantId, resourceType, resourceKey);
        BiResourceLockView lock = locks.get(ref);
        if (lock == null) {
            return unlocked(ref);
        }
        return lock;
    }
    /**
     * 执行 release Lock 相关处理。
     */
    public synchronized void releaseLock(Long tenantId, BiResourceLockCommand command) {
        if (command == null) {
            return;
        }
        ResourceRef ref = ref(tenantId, command.resourceType(), command.resourceKey());
        locks.remove(ref);
    }
    /**
     * 执行 update Location 相关处理。
     */
    public synchronized BiResourceLocationView updateLocation(Long tenantId,
                                                              BiResourceLocationCommand command,
                                                              String actor,
                                                              LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("BI resource location command is required");
        }
        return upsertLocation(ref(tenantId, command.resourceType(), command.resourceKey()),
                command.folderKey(), command.sortOrder(), actor, now);
    }
    /**
     * 执行 move 相关处理。
     */
    public synchronized BiResourceLocationView move(Long tenantId,
                                                    BiResourceMoveCommand command,
                                                    String actor,
                                                    LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("BI resource move command is required");
        }
        return upsertLocation(ref(tenantId, command.resourceType(), command.resourceKey()),
                command.folderKey(), command.sortOrder(), actor, now);
    }
    /**
     * 查询列表数据。
     */
    public synchronized List<BiResourceLocationView> listLocations(Long tenantId, String resourceType) {
        Long scopedTenantId = tenant(tenantId);
        String type = nullableType(resourceType);
        return locations.values().stream()
                .filter(location -> scopedTenantId.equals(location.tenantId()))
                .filter(location -> type == null || type.equals(location.resourceType()))
                .sorted(Comparator.comparing(BiResourceLocationView::folderKey)
                        .thenComparing(location -> location.sortOrder() == null ? 0 : location.sortOrder())
                        .thenComparing(BiResourceLocationView::resourceType)
                        .thenComparing(BiResourceLocationView::resourceKey))
                .toList();
    }
    /**
     * 执行 transfer 相关处理。
     */
    public synchronized BiResourceOwnershipView transfer(Long tenantId,
                                                         BiResourceTransferCommand command,
                                                         String actor,
                                                         LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("BI resource transfer command is required");
        }
        ResourceRef ref = ref(tenantId, command.resourceType(), command.resourceKey());
        BiResourceOwnershipView existing = ownerships.get(ref);
        BiResourceOwnershipView view = new BiResourceOwnershipView(
                existing == null ? nextOwnershipId++ : existing.id(),
                ref.tenantId(),
                WORKSPACE_ID,
                ref.resourceType(),
                ref.resourceKey(),
                requiredText(command.ownerUser(), "ownerUser"),
                actor(actor),
                now);
        ownerships.put(ref, view);
        return view;
    }
    /**
     * 查询列表数据。
     */
    public synchronized List<BiResourceOwnershipView> listOwnerships(Long tenantId, String resourceType) {
        Long scopedTenantId = tenant(tenantId);
        String type = nullableType(resourceType);
        return ownerships.values().stream()
                .filter(ownership -> scopedTenantId.equals(ownership.tenantId()))
                .filter(ownership -> type == null || type.equals(ownership.resourceType()))
                .sorted(Comparator.comparing(BiResourceOwnershipView::resourceType)
                        .thenComparing(BiResourceOwnershipView::resourceKey))
                .toList();
    }
    /**
     * 执行 request Approval 相关处理。
     */
    public synchronized BiPublishApprovalView requestApproval(Long tenantId,
                                                              BiPublishApprovalCommand command,
                                                              String actor,
                                                              LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("BI publish approval request is required");
        }
        ResourceRef ref = ref(tenantId, command.resourceType(), command.resourceKey());
        BiPublishApprovalView view = new BiPublishApprovalView(
                nextApprovalId++,
                ref.tenantId(),
                WORKSPACE_ID,
                ref.resourceType(),
                ref.resourceKey(),
                "PENDING",
                optionalText(command.reason()),
                actor(actor),
                now,
                null,
                null,
                null);
        approvals.put(view.id(), view);
        return view;
    }
    /**
     * 执行 review Approval 相关处理。
     */
    public synchronized BiPublishApprovalView reviewApproval(Long tenantId,
                                                             BiPublishApprovalReviewCommand command,
                                                             String actor,
                                                             LocalDateTime now) {
        if (command == null || command.approvalId() == null || command.approvalId() <= 0) {
            throw new IllegalArgumentException("approvalId is required");
        }
        BiPublishApprovalView existing = approvals.get(command.approvalId());
        if (existing == null || !tenant(tenantId).equals(existing.tenantId())) {
            throw new IllegalArgumentException("BI publish approval not found: " + command.approvalId());
        }
        if (!"PENDING".equals(existing.status())) {
            throw new IllegalStateException("BI publish approval is not pending: " + command.approvalId());
        }
        BiPublishApprovalView reviewed = new BiPublishApprovalView(
                existing.id(),
                existing.tenantId(),
                existing.workspaceId(),
                existing.resourceType(),
                existing.resourceKey(),
                reviewStatus(command.status()),
                existing.reason(),
                existing.requestedBy(),
                existing.requestedAt(),
                actor(actor),
                now,
                optionalText(command.reviewComment()));
        approvals.put(reviewed.id(), reviewed);
        return reviewed;
    }
    /**
     * 查询列表数据。
     */
    public synchronized List<BiPublishApprovalView> listApprovals(Long tenantId,
                                                                  String resourceType,
                                                                  String resourceKey,
                                                                  String status) {
        Long scopedTenantId = tenant(tenantId);
        String type = nullableType(resourceType);
        String key = nullableKey(resourceKey);
        String normalizedStatus = nullableStatus(status);
        return approvals.values().stream()
                .filter(approval -> scopedTenantId.equals(approval.tenantId()))
                .filter(approval -> type == null || type.equals(approval.resourceType()))
                .filter(approval -> key == null || key.equals(approval.resourceKey()))
                .filter(approval -> normalizedStatus == null || normalizedStatus.equals(approval.status()))
                .sorted(Comparator.comparing(BiPublishApprovalView::requestedAt).reversed()
                        .thenComparing(BiPublishApprovalView::id, Comparator.reverseOrder()))
                .toList();
    }
    /**
     * 创建或更新业务数据。
     */
    private BiResourceLocationView upsertLocation(ResourceRef ref,
                                                  String folderKey,
                                                  Integer sortOrder,
                                                  String actor,
                                                  LocalDateTime now) {
        BiResourceLocationView existing = locations.get(ref);
        BiResourceLocationView view = new BiResourceLocationView(
                existing == null ? nextLocationId++ : existing.id(),
                ref.tenantId(),
                WORKSPACE_ID,
                ref.resourceType(),
                ref.resourceKey(),
                folder(folderKey),
                sortOrder == null ? 0 : sortOrder,
                actor(actor),
                now);
        locations.put(ref, view);
        return view;
    }
    /**
     * 执行 unlocked 相关处理。
     */
    private static BiResourceLockView unlocked(ResourceRef ref) {
        return new BiResourceLockView(
                null,
                ref.tenantId(),
                WORKSPACE_ID,
                ref.resourceType(),
                ref.resourceKey(),
                null,
                null,
                null,
                null,
                false);
    }
    /**
     * 执行 ref 相关处理。
     */
    private static ResourceRef ref(Long tenantId, String resourceType, String resourceKey) {
        return new ResourceRef(tenant(tenantId), type(resourceType), key(resourceKey));
    }
    /**
     * 执行 tenant 相关处理。
     */
    private static Long tenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
    /**
     * 执行 type 相关处理。
     */
    private static String type(String resourceType) {
        String value = requiredText(resourceType, "resourceType").toUpperCase(Locale.ROOT);
        if (List.of("DATASET", "DASHBOARD", "CHART", "PORTAL", "BIG_SCREEN", "SPREADSHEET").contains(value)) {
            return value;
        }
        throw new IllegalArgumentException("unsupported BI resource type: " + resourceType);
    }
    /**
     * 执行 nullable Type 相关处理。
     */
    private static String nullableType(String resourceType) {
        return resourceType == null || resourceType.isBlank() ? null : type(resourceType);
    }
    /**
     * 执行 key 相关处理。
     */
    private static String key(String resourceKey) {
        return BiResourceKey.of(resourceKey, "resourceKey").value();
    }
    /**
     * 执行 nullable Key 相关处理。
     */
    private static String nullableKey(String resourceKey) {
        return resourceKey == null || resourceKey.isBlank() ? null : key(resourceKey);
    }
    /**
     * 执行 optional Key 相关处理。
     */
    private static String optionalKey(String value) {
        return value == null || value.isBlank() ? null : BiResourceKey.of(value, "widgetKey").value();
    }
    /**
     * 执行 folder 相关处理。
     */
    private static String folder(String value) {
        if (value == null || value.isBlank()) {
            return "root";
        }
        String normalized = value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s*/\\s*", "/")
                .replaceAll("[^a-z0-9/_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^[-/]+|[-/]+$)", "");
        return normalized.isBlank() ? "root" : normalized;
    }
    /**
     * 执行 required Text 相关处理。
     */
    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
    /**
     * 执行 optional Text 相关处理。
     */
    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
    /**
     * 执行 actor 相关处理。
     */
    private static String actor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
    /**
     * 执行 ttl 相关处理。
     */
    private static long ttl(Integer ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return 300L;
        }
        return Math.min(ttlSeconds.longValue(), 86_400L);
    }
    /**
     * 执行 review Status 相关处理。
     */
    private static String reviewStatus(String status) {
        String value = requiredText(status, "status").toUpperCase(Locale.ROOT);
        if ("APPROVED".equals(value) || "REJECTED".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("unsupported BI publish approval status: " + status);
    }
    /**
     * 执行 nullable Status 相关处理。
     */
    private static String nullableStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if ("PENDING".equals(value) || "APPROVED".equals(value) || "REJECTED".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("unsupported BI publish approval status: " + status);
    }
    /**
     * ResourceRef 不可变数据载体。
     */
    private record ResourceRef(Long tenantId, String resourceType, String resourceKey) {
    }
}
