package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.api.PlatformWorkstreamFacade;
import org.chovy.canvas.platform.api.WorkstreamStatusView;
import org.chovy.canvas.platform.domain.PlatformWorkstream;
import org.chovy.canvas.platform.domain.PlatformWorkstreamReadinessPolicy;
import org.chovy.canvas.platform.domain.PlatformWorkstreamRepository;
import org.chovy.canvas.platform.domain.WorkstreamKey;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 处理平台工作流状态查询和子规格准入校验的应用服务。
 */
@Service
public class PlatformWorkstreamApplicationService implements PlatformWorkstreamFacade {

    /**
     * 读取平台工作流定义的仓储。
     */
    private final PlatformWorkstreamRepository repository;

    /**
     * 使用工作流仓储创建应用服务。
     *
     * @param repository 读取平台工作流定义的仓储
     */
    public PlatformWorkstreamApplicationService(PlatformWorkstreamRepository repository) {
        this.repository = repository;
    }

    /**
     * 查询全部平台工作流状态。
     *
     * {@inheritDoc}
     */
    @Override
    public List<WorkstreamStatusView> statuses() {
        return repository.list().stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 校验指定工作流是否具备可执行子规格。
     *
     * {@inheritDoc}
     */
    @Override
    public WorkstreamStatusView requireExecutableChildSpec(String workstreamKey) {
        String normalizedKey = new WorkstreamKey(workstreamKey).value();
        PlatformWorkstream workstream = repository.get(normalizedKey);
        if (workstream == null) {
            throw new IllegalArgumentException("unknown workstream " + normalizedKey);
        }
        if (PlatformWorkstreamReadinessPolicy.requiresMissingChildSpec(workstream)) {
            // 需要子规格的工作流必须先补齐执行说明，避免直接进入实现阶段。
            throw new IllegalStateException(normalizedKey + " requires a child spec before implementation");
        }
        return toView(workstream);
    }

    /**
     * 将领域工作流转换为公开状态视图。
     *
     * @param workstream 领域工作流定义
     * @return 工作流状态视图
     */
    private WorkstreamStatusView toView(PlatformWorkstream workstream) {
        return new WorkstreamStatusView(
                workstream.workstreamKey(),
                workstream.displayName(),
                workstream.priority(),
                PlatformWorkstreamReadinessPolicy.statusFor(workstream),
                workstream.childSpecPath(),
                workstream.summary());
    }
}
