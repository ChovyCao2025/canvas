package org.chovy.canvas.platform;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
/**
 * PlatformWorkstreamService 汇总平台控制面能力、证据数据和交付状态。
 */
public class PlatformWorkstreamService {

    private final WorkstreamRepository repository;

    /**
     * 初始化 PlatformWorkstreamService 实例。
     *
     * @param repository 依赖组件，用于完成数据访问或外部能力调用。
     */
    public PlatformWorkstreamService(WorkstreamRepository repository) {
        this.repository = repository;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @return 返回 statuses 汇总后的集合、分页或映射视图。
     */
    public List<WorkstreamStatus> statuses() {
        return repository.list().stream()
                .map(this::toStatus)
                .toList();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param workstreamKey 业务键，用于在同一租户下定位资源。
     * @return 返回 requireExecutableChildSpec 流程生成的业务结果。
     */
    public Workstream requireExecutableChildSpec(String workstreamKey) {
        String normalizedKey = normalizeKey(workstreamKey);
        Workstream workstream = repository.get(normalizedKey);
        if (workstream == null) {
            throw new IllegalArgumentException("unknown workstream " + normalizedKey);
        }
        if (requiresMissingChildSpec(workstream)) {
            throw new IllegalStateException(normalizedKey + " requires a child spec before implementation");
        }
        return workstream;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param workstream workstream 参数，用于 toStatus 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private WorkstreamStatus toStatus(Workstream workstream) {
        String status = requiresMissingChildSpec(workstream)
                ? "BLOCKED_CHILD_SPEC_REQUIRED"
                : "READY_FOR_CHILD_EXECUTION";
        return new WorkstreamStatus(
                workstream.workstreamKey(),
                workstream.displayName(),
                workstream.priority(),
                status,
                workstream.childSpecPath(),
                workstream.summary());
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param workstream workstream 参数，用于 requiresMissingChildSpec 流程中的校验、计算或对象转换。
     * @return 返回 requires missing child spec 的布尔判断结果。
     */
    private static boolean requiresMissingChildSpec(Workstream workstream) {
        return workstream.requiresChildSpec()
                && (workstream.childSpecPath() == null || workstream.childSpecPath().isBlank());
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param workstreamKey 业务键，用于在同一租户下定位资源。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeKey(String workstreamKey) {
        String normalized = Objects.requireNonNull(workstreamKey, "workstreamKey")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9-]{0,127}")) {
            throw new IllegalArgumentException("invalid workstream key: " + workstreamKey);
        }
        return normalized;
    }

    /**
     * Workstream 汇总平台控制面能力、证据数据和交付状态。
     */
    public record Workstream(
            String workstreamKey,
            String displayName,
            String priority,
            boolean requiresChildSpec,
            String childSpecPath,
            String summary) {
    }

    /**
     * WorkstreamStatus 汇总平台控制面能力、证据数据和交付状态。
     */
    public record WorkstreamStatus(
            String workstreamKey,
            String displayName,
            String priority,
            String status,
            String childSpecPath,
            String summary) {
    }

    /**
     * WorkstreamRepository 汇总平台控制面能力、证据数据和交付状态。
     */
    public interface WorkstreamRepository {
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @return 返回符合条件的数据列表或视图。
         */
        List<Workstream> list();

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param workstreamKey 业务键，用于在同一租户下定位资源。
         * @return 返回 get 流程生成的业务结果。
         */
        Workstream get(String workstreamKey);
    }
}
