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

    public CanvasPrePublishCheckService(CanvasVersionMapper canvasVersionMapper, DagParser dagParser) {
        this.canvasVersionMapper = canvasVersionMapper;
        this.dagParser = dagParser;
    }

    public Result check(Long canvasId) {
        List<CheckItem> items = new ArrayList<>();
        CanvasVersionDO draft = latestDraft(canvasId);
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
        return result(items);
    }

    private CanvasVersionDO latestDraft(Long canvasId) {
        return canvasVersionMapper.selectOne(new QueryWrapper<CanvasVersionDO>()
                .eq("canvas_id", canvasId)
                .eq("status", VersionStatus.DRAFT.getCode())
                .orderByDesc("version")
                .last("LIMIT 1"));
    }

    private boolean hasTriggerEntry(DagGraph graph) {
        return graph.entryNodes().stream()
                .map(graph::getNode)
                .anyMatch(node -> node != null && TRIGGER_NODE_TYPES.contains(node.getType()));
    }

    private Result result(List<CheckItem> items) {
        boolean blocking = items.stream().anyMatch(item -> ERROR.equals(item.severity()));
        return new Result(blocking, List.copyOf(items));
    }

    public record CheckItem(String code, String severity, String message) {
    }

    public record Result(boolean blocking, List<CheckItem> items) {
        public Result {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }
}
