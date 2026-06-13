package org.chovy.canvas.marketing.application.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    public AuditResult audit(AuditRequest request) {
        List<JourneyNode> nodes = request.nodes();
        List<JourneyEdge> edges = request.edges();
        List<RiskFinding> findings = new ArrayList<>();

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

    private static boolean containsAnyKey(Map<String, Object> config, Set<String> expectedKeys) {
        return config.keySet().stream()
                .map(key -> key.toLowerCase(Locale.ROOT))
                .anyMatch(expectedKeys::contains);
    }

    private static RiskFinding finding(String code,
                                       String severity,
                                       String nodeId,
                                       String message,
                                       String recommendation) {
        return new RiskFinding(code, severity, nodeId, message, recommendation);
    }

    public record AuditRequest(
            Long tenantId,
            String journeyKey,
            List<JourneyNode> nodes,
            List<JourneyEdge> edges) {

        public AuditRequest {
            if (tenantId == null || tenantId <= 0) {
                throw new IllegalArgumentException("tenantId is required");
            }
            journeyKey = journeyKey == null ? "" : journeyKey.trim();
            nodes = List.copyOf(nodes == null ? List.of() : nodes);
            edges = List.copyOf(edges == null ? List.of() : edges);
        }
    }

    public record JourneyNode(String id, String type, Map<String, Object> config) {

        public JourneyNode {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id is required");
            }
            type = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
            config = Map.copyOf(config == null ? Map.of() : config);
        }
    }

    public record JourneyEdge(String from, String to) {

        public JourneyEdge {
            from = from == null ? "" : from.trim();
            to = to == null ? "" : to.trim();
        }
    }

    public record RiskFinding(
            String code,
            String severity,
            String nodeId,
            String message,
            String recommendation) {

        public RiskFinding {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("code is required");
            }
            severity = severity == null || severity.isBlank() ? "WARNING" : severity;
            nodeId = nodeId == null ? "" : nodeId;
            message = message == null ? "" : message;
            recommendation = recommendation == null ? "" : recommendation;
        }
    }

    public record AuditResult(String auditStatus, boolean safeForPreview, List<RiskFinding> findings) {

        public AuditResult {
            auditStatus = auditStatus == null || auditStatus.isBlank() ? "UNKNOWN" : auditStatus;
            findings = List.copyOf(findings == null ? List.of() : findings);
        }
    }
}
