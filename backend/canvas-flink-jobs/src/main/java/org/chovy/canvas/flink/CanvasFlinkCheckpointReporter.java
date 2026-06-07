package org.chovy.canvas.flink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class CanvasFlinkCheckpointReporter {

    private final URI endpoint;
    private final Duration timeout;
    private final String internalApiToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CanvasFlinkCheckpointReporter(String endpoint, Duration timeout) {
        this(endpoint, timeout, "");
    }

    public CanvasFlinkCheckpointReporter(String endpoint, Duration timeout, String internalApiToken) {
        this(URI.create(required(endpoint, "checkpoint endpoint")),
                timeout == null ? Duration.ofSeconds(5) : timeout,
                internalApiToken,
                HttpClient.newBuilder()
                        .connectTimeout(timeout == null ? Duration.ofSeconds(5) : timeout)
                        .build(),
                new ObjectMapper());
    }

    CanvasFlinkCheckpointReporter(URI endpoint,
                                  Duration timeout,
                                  String internalApiToken,
                                  HttpClient httpClient,
                                  ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        this.timeout = timeout;
        this.internalApiToken = internalApiToken == null ? "" : internalApiToken.trim();
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public void report(CheckpointPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("checkpoint payload is required");
        }
        String body = toJson(payload);
        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (!internalApiToken.isBlank()) {
            builder.header("X-Canvas-Internal-Token", internalApiToken);
        }
        HttpRequest request = builder.build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("checkpoint report failed with HTTP "
                        + response.statusCode() + ": " + response.body());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("checkpoint report request failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("checkpoint report request interrupted", ex);
        }
    }

    private String toJson(CheckpointPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("checkpoint payload cannot be serialized", ex);
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public record CheckpointPayload(
            String pipelineKey,
            String checkpointId,
            String sourcePartition,
            String sourceOffset,
            String committedOffset,
            String watermarkTime,
            String checkpointTime,
            Long lagMs,
            Long rowCount,
            String status,
            String errorMessage,
            String reportedBy,
            String sourceSchemaVersion,
            String sinkSchemaVersion) {
    }
}
