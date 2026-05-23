package org.chovy.canvas.engine.dag;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DAG 只读视图。
 *
 * <p>约定：
 * <ul>
 *   <li>构造由 {@link DagParser} 完成，运行期不做结构变更。</li>
 *   <li>forward/reverse/inDegree 三份索引在解析阶段一次性构建，
 *       以空间换时间，减少执行期重复计算。</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public class DagGraph {
    /** nodeId -> 节点定义（包含 type/name/config/bizConfig） */
    private final Map<String, DagParser.CanvasNode> nodeMap;
    /** 正向邻接表：nodeId -> 所有下游节点 */
    private final Map<String, List<String>> forward;   // nodeId → [下游 nodeId]
    /** 反向邻接表：nodeId -> 所有上游节点 */
    private final Map<String, List<String>> reverse;   // nodeId → [上游 nodeId]
    /** 入度索引：用于入口节点判定和环检测结果落地 */
    private final Map<String, Integer> inDegree;

    /** 按 nodeId 获取节点定义。 */
    public DagParser.CanvasNode getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

    /** 查询下游节点列表；不存在时返回空列表。 */
    public List<String> downstream(String nodeId) {
        return forward.getOrDefault(nodeId, List.of());
    }

    /** 查询上游节点列表；不存在时返回空列表。 */
    public List<String> upstream(String nodeId) {
        return reverse.getOrDefault(nodeId, List.of());
    }

    /** 找入边为 0 的节点（触发器节点） */
    public List<String> entryNodes() {
        return inDegree.entrySet().stream()
                .filter(e -> e.getValue() == 0)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** 返回图中全部节点 ID */
    public Set<String> allNodeIds() {
        return nodeMap.keySet();
    }
}
