package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
/**
 * CanvasPrePublishCheckService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CanvasPrePublishCheckService {

    private static final String ERROR = "ERROR";
    private static final String WARNING = "WARNING";
    private static final Set<String> TRIGGER_NODE_TYPES = Set.of(
            NodeType.EVENT_TRIGGER,
            NodeType.MQ_TRIGGER,
            NodeType.SCHEDULED_TRIGGER,
            NodeType.DIRECT_CALL);

    private final CanvasVersionMapper canvasVersionMapper;
    private final DagParser dagParser;

    /**
     * 初始化 CanvasPrePublishCheckService 实例。
     *
     * @param canvasVersionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dagParser dag parser 参数，用于 CanvasPrePublishCheckService 流程中的校验、计算或对象转换。
     */
    public CanvasPrePublishCheckService(CanvasVersionMapper canvasVersionMapper, DagParser dagParser) {
        this.canvasVersionMapper = canvasVersionMapper;
        this.dagParser = dagParser;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回布尔判断结果。
     */
    public Result check(Long canvasId) {
        // 准备本次处理所需的上下文和中间变量。
        List<CheckItem> items = new ArrayList<>();
        CanvasVersionDO draft = latestDraft(canvasId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (draft == null) {
            items.add(new CheckItem("NO_DRAFT_VERSION", ERROR, "没有可发布的草稿版本"));
            return result(items);
        }
        DagGraph graph;
        try {
            graph = dagParser.parse(draft.getGraphJson());
        } catch (IllegalArgumentException e) {
            items.add(new CheckItem("GRAPH_JSON_INVALID", ERROR, e.getMessage()));
            return result(items);
        }
        if (!hasTriggerEntry(graph)) {
            items.add(new CheckItem("NO_ENTRY_NODE", ERROR, "画布缺少 EVENT/MQ/定时触发器入口节点"));
        }
        items.add(new CheckItem("NO_TEST_SEND", WARNING, "尚未找到发布前测试发送证据"));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result(items);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回 latestDraft 流程生成的业务结果。
     */
    private CanvasVersionDO latestDraft(Long canvasId) {
        return canvasVersionMapper.selectOne(new QueryWrapper<CanvasVersionDO>()
                .eq("canvas_id", canvasId)
                .eq("status", VersionStatus.DRAFT.getCode())
                .orderByDesc("version")
                .last("LIMIT 1"));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param graph graph 参数，用于 hasTriggerEntry 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasTriggerEntry(DagGraph graph) {
        return graph.entryNodes().stream()
                .map(graph::getNode)
                .anyMatch(node -> node != null && TRIGGER_NODE_TYPES.contains(node.getType()));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param items items 参数，用于 result 流程中的校验、计算或对象转换。
     * @return 返回 result 流程生成的业务结果。
     */
    private Result result(List<CheckItem> items) {
        boolean blocking = items.stream().anyMatch(item -> ERROR.equals(item.severity()));
        return new Result(blocking, List.copyOf(items));
    }

    /**
     * CheckItem 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record CheckItem(String code, String severity, String message) {
    }

    /**
     * Result 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record Result(boolean blocking, List<CheckItem> items) {
        public Result {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }
}
