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

public class BiResourceOperationsCatalog {

    private static final Long WORKSPACE_ID = 5L;
    private final Map<Long, BiResourceCommentView> comments = new LinkedHashMap<>();
    private final Map<ResourceRef, BiResourceLockView> locks = new LinkedHashMap<>();
    private final Map<ResourceRef, BiResourceLocationView> locations = new LinkedHashMap<>();
    private final Map<ResourceRef, BiResourceOwnershipView> ownerships = new LinkedHashMap<>();
    private final Map<Long, BiPublishApprovalView> approvals = new LinkedHashMap<>();
    private long nextCommentId = 1L;
    private long nextLockId = 1L;
    private long nextLocationId = 1L;
    private long nextOwnershipId = 1L;
    private long nextApprovalId = 1L;

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

    public synchronized void deleteComment(Long tenantId, Long commentId, LocalDateTime now) {
        if (commentId == null || commentId <= 0) {
            return;
        }
        BiResourceCommentView existing = comments.get(commentId);
        if (existing == null || !tenant(tenantId).equals(existing.tenantId()) || existing.deletedAt() != null) {
            return;
        }
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

    public synchronized BiResourceLockView acquireLock(Long tenantId,
                                                       BiResourceLockCommand command,
                                                       String actor,
                                                       LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("BI resource lock command is required");
        }
        ResourceRef ref = ref(tenantId, command.resourceType(), command.resourceKey());
        BiResourceLockView existing = locks.get(ref);
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

    public synchronized BiResourceLockView currentLock(Long tenantId, String resourceType, String resourceKey) {
        ResourceRef ref = ref(tenantId, resourceType, resourceKey);
        BiResourceLockView lock = locks.get(ref);
        if (lock == null) {
            return unlocked(ref);
        }
        return lock;
    }

    public synchronized void releaseLock(Long tenantId, BiResourceLockCommand command) {
        if (command == null) {
            return;
        }
        ResourceRef ref = ref(tenantId, command.resourceType(), command.resourceKey());
        locks.remove(ref);
    }

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

    private static ResourceRef ref(Long tenantId, String resourceType, String resourceKey) {
        return new ResourceRef(tenant(tenantId), type(resourceType), key(resourceKey));
    }

    private static Long tenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String type(String resourceType) {
        String value = requiredText(resourceType, "resourceType").toUpperCase(Locale.ROOT);
        if (List.of("DATASET", "DASHBOARD", "CHART", "PORTAL", "BIG_SCREEN", "SPREADSHEET").contains(value)) {
            return value;
        }
        throw new IllegalArgumentException("unsupported BI resource type: " + resourceType);
    }

    private static String nullableType(String resourceType) {
        return resourceType == null || resourceType.isBlank() ? null : type(resourceType);
    }

    private static String key(String resourceKey) {
        return BiResourceKey.of(resourceKey, "resourceKey").value();
    }

    private static String nullableKey(String resourceKey) {
        return resourceKey == null || resourceKey.isBlank() ? null : key(resourceKey);
    }

    private static String optionalKey(String value) {
        return value == null || value.isBlank() ? null : BiResourceKey.of(value, "widgetKey").value();
    }

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

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String actor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static long ttl(Integer ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return 300L;
        }
        return Math.min(ttlSeconds.longValue(), 86_400L);
    }

    private static String reviewStatus(String status) {
        String value = requiredText(status, "status").toUpperCase(Locale.ROOT);
        if ("APPROVED".equals(value) || "REJECTED".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("unsupported BI publish approval status: " + status);
    }

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

    private record ResourceRef(Long tenantId, String resourceType, String resourceKey) {
    }
}
