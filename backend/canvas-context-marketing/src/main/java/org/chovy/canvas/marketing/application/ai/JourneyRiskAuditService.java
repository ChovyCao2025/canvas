package org.chovy.canvas.marketing.application.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/**
 * 提供JourneyRiskAuditService的业务能力。
 */
public class JourneyRiskAuditService {

    private static final Set<String> TOUCH_NODE_TYPES = Set.of("message", "coupon", "ai");
    private static final Set<String> FREQUENCY_CAP_KEYS = Set.of(
            "frequencycap",
            "frequency_cap",
            "cooldown",
            "cooldownhours",
            "peruserlimit",
            "per_user_limit",
            "dailylimit");
    private static final Set<String> COUPON_CAP_KEYS = Set.of(
            "maxredemptions",
            "maxtotal",
            "limit",
            "couponlimit",
            "peruserlimit",
            "per_user_limit");

    /**
     * 执行audit业务操作。
     */
    public AuditResult audit(AuditRequest request) {
        List<JourneyNode> nodes = request.nodes();
        List<JourneyEdge> edges = request.edges();
        List<RiskFinding> findings = new ArrayList<>();

        // 触达类节点必须配合频控，否则预览阶段就返回阻断项，避免高频营销误上线。
        boolean hasTouchNode = nodes.stream().anyMatch(node -> TOUCH_NODE_TYPES.contains(node.type()));
        boolean hasFrequencyCap = nodes.stream().anyMatch(node -> containsAnyKey(node.config(), FREQUENCY_CAP_KEYS));
        if (hasTouchNode && !hasFrequencyCap) {
            findings.add(finding(
                    "MISSING_FREQUENCY_CAP",
                    "BLOCKER",
                    "",
                    "Journey has touch nodes but no frequency cap or cooldown.",
                    "Add a per-user cap, daily cap, or cooldown before launch."));
        }

        boolean hasApproval = nodes.stream().anyMatch(node -> "approval".equals(node.type()));
        if (!hasApproval) {
            findings.add(finding(
                    "MISSING_APPROVAL",
                    "BLOCKER",
                    "",
                    "Journey has no approval node.",
                    "Route campaign launch through an approval step."));
        }

        // 优惠券节点单独检查总量或用户级限制，避免只依赖全旅程频控遗漏核销风险。
        nodes.stream()
                .filter(node -> "coupon".equals(node.type()))
                .filter(node -> !containsAnyKey(node.config(), COUPON_CAP_KEYS))
                .findFirst()
                .ifPresent(node -> findings.add(finding(
                        "COUPON_WITHOUT_CAP",
                        "BLOCKER",
                        node.id(),
                        "Coupon node has no redemption cap.",
                        "Set maxRedemptions, maxTotal, limit, or perUserLimit.")));

        long touchCount = nodes.stream().filter(node -> TOUCH_NODE_TYPES.contains(node.type())).count();
        if (touchCount >= 3 && !hasFrequencyCap) {
            findings.add(finding(
                    "TOUCHES_TOO_DENSE",
                    "WARNING",
                    "",
                    "Journey contains multiple touch nodes without spacing controls.",
                    "Add cooldown or split touches with explicit exit criteria."));
        }

        if (!hasExitPath(nodes, edges)) {
            findings.add(finding(
                    "MISSING_FAILURE_OR_EXIT_PATH",
                    "BLOCKER",
                    "",
                    "Journey has no explicit exit path.",
                    "Connect successful or failed branches to an end node."));
        }

        return new AuditResult(findings.isEmpty() ? "READY" : "BLOCKED", true, findings);
    }

    /**
     * 判断hasExitPath条件是否成立。
     */
    private static boolean hasExitPath(List<JourneyNode> nodes, List<JourneyEdge> edges) {
        Set<String> endNodeIds = nodes.stream()
                .filter(node -> "end".equals(node.type()))
                .map(JourneyNode::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (endNodeIds.isEmpty()) {
            return false;
        }
        return edges.stream().anyMatch(edge -> endNodeIds.contains(edge.to()));
    }

    /**
     * 执行containsAnyKey业务操作。
     */
    private static boolean containsAnyKey(Map<String, Object> config, Set<String> expectedKeys) {
        return config.keySet().stream()
                .map(key -> key.toLowerCase(Locale.ROOT))
                .anyMatch(expectedKeys::contains);
    }

    /**
     * 查找ing业务对象。
     */
    private static RiskFinding finding(String code,
                                       String severity,
                                       String nodeId,
                                       String message,
                                       String recommendation) {
        return new RiskFinding(code, severity, nodeId, message, recommendation);
    }

    /**
     * 表示AuditRequest的数据结构。
     */
    public static final class AuditRequest {

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * 旅程业务键。
         */
        private final String journeyKey;

        /**
         * 旅程节点列表。
         */
        private final List<JourneyNode> nodes;

        /**
         * 旅程边列表。
         */
        private final List<JourneyEdge> edges;

        /**
         * 创建AuditRequest实例。
         */
        public AuditRequest(Long tenantId, String journeyKey, List<JourneyNode> nodes, List<JourneyEdge> edges) {
            if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
            }
            journeyKey = journeyKey == null ? "" : journeyKey.trim();
            nodes = List.copyOf(nodes == null ? List.of() : nodes);
            edges = List.copyOf(edges == null ? List.of() : edges);

            this.tenantId = tenantId;
            this.journeyKey = journeyKey;
            this.nodes = nodes;
            this.edges = edges;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回旅程业务键。
         */
        public String journeyKey() {
            return journeyKey;
        }

        /**
         * 返回旅程节点列表。
         */
        public List<JourneyNode> nodes() {
            return nodes;
        }

        /**
         * 返回旅程边列表。
         */
        public List<JourneyEdge> edges() {
            return edges;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AuditRequest that = (AuditRequest) o;
            return                     Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(journeyKey, that.journeyKey) &&
                    Objects.equals(nodes, that.nodes) &&
                    Objects.equals(edges, that.edges);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, journeyKey, nodes, edges);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "AuditRequest[tenantId=" + tenantId + ", journeyKey=" + journeyKey + ", nodes=" + nodes + ", edges=" + edges + "]";
        }
    }

    /**
     * 表示JourneyNode的数据结构。
     */
    public static final class JourneyNode {

        /**
         * 记录的唯一标识。
         */
        private final String id;

        /**
         * 类型标识。
         */
        private final String type;

        /**
         * 节点配置。
         */
        private final Map<String, Object> config;

        /**
         * 创建JourneyNode实例。
         */
        public JourneyNode(String id, String type, Map<String, Object> config) {
            if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
            }
            type = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
            config = Map.copyOf(config == null ? Map.of() : config);

            this.id = id;
            this.type = type;
            this.config = config;
        }

        /**
         * 返回记录的唯一标识。
         */
        public String id() {
            return id;
        }

        /**
         * 返回类型标识。
         */
        public String type() {
            return type;
        }

        /**
         * 返回节点配置。
         */
        public Map<String, Object> config() {
            return config;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            JourneyNode that = (JourneyNode) o;
            return                     Objects.equals(id, that.id) &&
                    Objects.equals(type, that.type) &&
                    Objects.equals(config, that.config);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(id, type, config);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "JourneyNode[id=" + id + ", type=" + type + ", config=" + config + "]";
        }
    }

    /**
     * 表示JourneyEdge的数据结构。
     */
    public static final class JourneyEdge {

        /**
         * 边的起点节点标识。
         */
        private final String from;

        /**
         * 边的终点节点标识。
         */
        private final String to;

        /**
         * 创建JourneyEdge实例。
         */
        public JourneyEdge(String from, String to) {
            from = from == null ? "" : from.trim();
            to = to == null ? "" : to.trim();

            this.from = from;
            this.to = to;
        }

        /**
         * 返回边的起点节点标识。
         */
        public String from() {
            return from;
        }

        /**
         * 返回边的终点节点标识。
         */
        public String to() {
            return to;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            JourneyEdge that = (JourneyEdge) o;
            return                     Objects.equals(from, that.from) &&
                    Objects.equals(to, that.to);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "JourneyEdge[from=" + from + ", to=" + to + "]";
        }
    }

    /**
     * 描述RiskFinding中的单个发现。
     */
    public static final class RiskFinding {

        /**
         * 风险编码。
         */
        private final String code;

        /**
         * 问题严重级别。
         */
        private final String severity;

        /**
         * nodeId 字段值。
         */
        private final String nodeId;

        /**
         * 面向调用方的提示信息。
         */
        private final String message;

        /**
         * 处理建议。
         */
        private final String recommendation;

        /**
         * 创建RiskFinding实例。
         */
        public RiskFinding(String code, String severity, String nodeId, String message, String recommendation) {
            if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
            }
            severity = severity == null || severity.isBlank() ? "WARNING" : severity;
            nodeId = nodeId == null ? "" : nodeId;
            message = message == null ? "" : message;
            recommendation = recommendation == null ? "" : recommendation;

            this.code = code;
            this.severity = severity;
            this.nodeId = nodeId;
            this.message = message;
            this.recommendation = recommendation;
        }

        /**
         * 返回风险编码。
         */
        public String code() {
            return code;
        }

        /**
         * 返回问题严重级别。
         */
        public String severity() {
            return severity;
        }

        /**
         * 返回nodeId 字段值。
         */
        public String nodeId() {
            return nodeId;
        }

        /**
         * 返回面向调用方的提示信息。
         */
        public String message() {
            return message;
        }

        /**
         * 返回处理建议。
         */
        public String recommendation() {
            return recommendation;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RiskFinding that = (RiskFinding) o;
            return                     Objects.equals(code, that.code) &&
                    Objects.equals(severity, that.severity) &&
                    Objects.equals(nodeId, that.nodeId) &&
                    Objects.equals(message, that.message) &&
                    Objects.equals(recommendation, that.recommendation);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(code, severity, nodeId, message, recommendation);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "RiskFinding[code=" + code + ", severity=" + severity + ", nodeId=" + nodeId + ", message=" + message + ", recommendation=" + recommendation + "]";
        }
    }

    /**
     * 承载AuditResult处理结果。
     */
    public static final class AuditResult {

        /**
         * 审计状态。
         */
        private final String auditStatus;

        /**
         * 是否允许进入预览。
         */
        private final boolean safeForPreview;

        /**
         * 风险发现列表。
         */
        private final List<RiskFinding> findings;

        /**
         * 创建AuditResult实例。
         */
        public AuditResult(String auditStatus, boolean safeForPreview, List<RiskFinding> findings) {
            auditStatus = auditStatus == null || auditStatus.isBlank() ? "UNKNOWN" : auditStatus;
            findings = List.copyOf(findings == null ? List.of() : findings);

            this.auditStatus = auditStatus;
            this.safeForPreview = safeForPreview;
            this.findings = findings;
        }

        /**
         * 返回审计状态。
         */
        public String auditStatus() {
            return auditStatus;
        }

        /**
         * 返回是否允许进入预览。
         */
        public boolean safeForPreview() {
            return safeForPreview;
        }

        /**
         * 返回风险发现列表。
         */
        public List<RiskFinding> findings() {
            return findings;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AuditResult that = (AuditResult) o;
            return                     Objects.equals(auditStatus, that.auditStatus) &&
                    safeForPreview == that.safeForPreview &&
                    Objects.equals(findings, that.findings);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(auditStatus, safeForPreview, findings);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "AuditResult[auditStatus=" + auditStatus + ", safeForPreview=" + safeForPreview + ", findings=" + findings + "]";
        }
    }
}
