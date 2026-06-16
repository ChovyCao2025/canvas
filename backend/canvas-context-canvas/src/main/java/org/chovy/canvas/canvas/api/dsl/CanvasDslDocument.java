package org.chovy.canvas.canvas.api.dsl;

import java.util.List;
import java.util.Map;

/**
 * 承载CanvasDslDocument的数据快照。
 */
public record CanvasDslDocument(
        /**
         * 记录apiVersion。
         */
        String apiVersion,
        /**
         * 记录kind。
         */
        String kind,
        /**
         * 记录元数据。
         */
        Metadata metadata,
        /**
         * 记录spec。
         */
        Spec spec) {

    public CanvasDslDocument {
        apiVersion = requireText(apiVersion, "apiVersion");
        kind = requireText(kind, "kind");
        metadata = metadata == null ? new Metadata("", "") : metadata;
        spec = spec == null ? new Spec(new Trigger("", ""), List.of(), List.of()) : spec;
    }

    /**
     * 承载元数据的数据快照。
     */
    public record Metadata(String name, String title) {

        public Metadata {
            name = name == null ? "" : name;
            title = title == null ? "" : title;
        }
    }

    /**
     * 承载Spec的数据快照。
     */
    public record Spec(Trigger trigger, List<Node> nodes, List<Edge> edges) {

        public Spec {
            trigger = trigger == null ? new Trigger("", "") : trigger;
            nodes = List.copyOf(nodes == null ? List.of() : nodes);
            edges = List.copyOf(edges == null ? List.of() : edges);
        }
    }

    /**
     * 承载Trigger的数据快照。
     */
    public record Trigger(String type, String event) {

        public Trigger {
            type = type == null ? "" : type;
            event = event == null ? "" : event;
        }
    }

    /**
     * 承载Node的数据快照。
     */
    public record Node(String id, String type, Map<String, Object> config) {

        public Node {
            id = id == null ? "" : id;
            type = type == null ? "" : type;
            config = Map.copyOf(config == null ? Map.of() : config);
        }
    }

    /**
     * 承载Edge的数据快照。
     */
    public record Edge(String from, String to) {

        public Edge {
            from = from == null ? "" : from;
            to = to == null ? "" : to;
        }
    }

    /**
     * 校验文本不能为空。
     */
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
