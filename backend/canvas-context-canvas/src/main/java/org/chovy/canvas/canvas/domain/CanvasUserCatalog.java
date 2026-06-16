package org.chovy.canvas.canvas.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.chovy.canvas.canvas.api.CanvasUserFacade.CanvasExecutionView;
import org.chovy.canvas.canvas.api.CanvasUserFacade.CanvasUserCommand;
import org.chovy.canvas.canvas.api.CanvasUserFacade.CanvasUserView;
import org.chovy.canvas.canvas.api.CanvasUserFacade.ExecutionCommand;

/**
 * 封装CanvasUserCatalog相关的业务逻辑。
 */
public class CanvasUserCatalog {

    /**
     * 保存内存实现使用的users映射数据。
     */
    private final Map<CanvasUserKey, CanvasUserView> users = new LinkedHashMap<>();

    /**
     * 保存测试或内存实现使用的executions列表。
     */
    private final List<CanvasExecutionView> executions = new ArrayList<>();

    /**
     * 保存executionIds。
     */
    private long executionIds;

    /**
     * 列出Users。
     */
    public synchronized List<CanvasUserView> listUsers(Long canvasId) {
        Long scopedCanvasId = requireCanvasId(canvasId);
        return users.values().stream()
                .filter(row -> Objects.equals(row.canvasId(), scopedCanvasId))
                .sorted(Comparator.comparing(CanvasUserView::userId))
                .toList();
    }

    /**
     * 获取UserInCanvas。
     */
    public synchronized CanvasUserView getUserInCanvas(Long canvasId, String userId) {
        Long scopedCanvasId = requireCanvasId(canvasId);
        String scopedUserId = requireText(userId, "user id");
        CanvasUserView row = users.get(new CanvasUserKey(scopedCanvasId, scopedUserId));
        if (row == null) {
            throw new IllegalArgumentException("canvas user is not found");
        }
        return row;
    }

    /**
     * 列出Executions。
     */
    public synchronized List<CanvasExecutionView> listExecutions(Long canvasId, String userId) {
        Long scopedCanvasId = requireCanvasId(canvasId);
        String scopedUserId = requireText(userId, "user id");
        return executions.stream()
                .filter(row -> Objects.equals(row.canvasId(), scopedCanvasId))
                .filter(row -> Objects.equals(row.userId(), scopedUserId))
                .sorted(Comparator.comparing(CanvasExecutionView::id))
                .toList();
    }

    /**
     * 处理registerUser。
     */
    public synchronized void registerUser(Long canvasId, CanvasUserCommand command) {
        Long scopedCanvasId = requireCanvasId(canvasId);
        if (command == null) {
            throw new IllegalArgumentException("canvas user command is required");
        }
        String userId = requireText(command.userId(), "user id");
        CanvasUserView row = new CanvasUserView(scopedCanvasId, userId, optional(command.email()),
                optional(command.mobile()), defaultText(command.touchStatus(), "ENTERED"),
                command.profile() == null ? Map.of() : Map.copyOf(command.profile()));
        users.put(new CanvasUserKey(scopedCanvasId, userId), row);
    }

    /**
     * 处理registerExecution。
     */
    public synchronized void registerExecution(Long canvasId, String userId, ExecutionCommand command) {
        Long scopedCanvasId = requireCanvasId(canvasId);
        String scopedUserId = requireText(userId, "user id");
        if (command == null) {
            throw new IllegalArgumentException("execution command is required");
        }
        executions.add(new CanvasExecutionView(++executionIds, scopedCanvasId, scopedUserId, command.nodeId(),
                optional(command.nodeKey()), defaultText(command.status(), "SUCCESS"), command.executedAt()));
    }

    /**
     * 校验并返回画布标识。
     */
    private static Long requireCanvasId(Long canvasId) {
        if (canvasId == null) {
            throw new IllegalArgumentException("canvas id is required");
        }
        return canvasId;
    }

    /**
     * 校验并返回Text。
     */
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 处理optional。
     */
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 处理defaultText。
     */
    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 承载CanvasUserKey的数据快照。
     */
    private record CanvasUserKey(Long canvasId, String userId) {
    }
}
