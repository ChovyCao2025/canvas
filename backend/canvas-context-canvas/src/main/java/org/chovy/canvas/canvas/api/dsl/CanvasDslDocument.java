package org.chovy.canvas.canvas.api.dsl;

import java.util.List;
import java.util.Map;

public record CanvasDslDocument(
        String apiVersion,
        String kind,
        Metadata metadata,
        Spec spec) {

    public CanvasDslDocument {
        apiVersion = requireText(apiVersion, "apiVersion");
        kind = requireText(kind, "kind");
        metadata = metadata == null ? new Metadata("", "") : metadata;
        spec = spec == null ? new Spec(new Trigger("", ""), List.of(), List.of()) : spec;
    }

    public record Metadata(String name, String title) {

        public Metadata {
            name = name == null ? "" : name;
            title = title == null ? "" : title;
        }
    }

    public record Spec(Trigger trigger, List<Node> nodes, List<Edge> edges) {

        public Spec {
            trigger = trigger == null ? new Trigger("", "") : trigger;
            nodes = List.copyOf(nodes == null ? List.of() : nodes);
            edges = List.copyOf(edges == null ? List.of() : edges);
        }
    }

    public record Trigger(String type, String event) {

        public Trigger {
            type = type == null ? "" : type;
            event = event == null ? "" : event;
        }
    }

    public record Node(String id, String type, Map<String, Object> config) {

        public Node {
            id = id == null ? "" : id;
            type = type == null ? "" : type;
            config = Map.copyOf(config == null ? Map.of() : config);
        }
    }

    public record Edge(String from, String to) {

        public Edge {
            from = from == null ? "" : from;
            to = to == null ? "" : to;
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
