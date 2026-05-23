package org.chovy.canvas.perf.mq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PerfMqProducer {
    private static final String DEFAULT_NAME_SERVER = "localhost:9876";
    private static final String DEFAULT_TOPIC = "CANVAS_MQ_TRIGGER";
    private static final String DEFAULT_TAG = "PERF_MQ";
    private static final int DEFAULT_COUNT = 1000;
    private static final int DEFAULT_USER_MODULO = 1000;

    public static void main(String[] args) throws Exception {
        Map<String, String> parsedArgs = parseArgs(args);
        String nameServer = parsedArgs.getOrDefault("--name-server", DEFAULT_NAME_SERVER);
        String topic = parsedArgs.getOrDefault("--topic", DEFAULT_TOPIC);
        String tag = parsedArgs.getOrDefault("--tag", DEFAULT_TAG);
        String perfRunId = require(parsedArgs, "--perf-run-id");
        int count = parseInt(parsedArgs.getOrDefault("--count", Integer.toString(DEFAULT_COUNT)), "--count");
        int userModulo = parseInt(parsedArgs.getOrDefault("--user-modulo", Integer.toString(DEFAULT_USER_MODULO)), "--user-modulo");

        DefaultMQProducer producer = new DefaultMQProducer("PID_CANVAS_PERF");
        producer.setNamesrvAddr(nameServer);
        try {
            producer.start();
            for (int seq = 0; seq < count; seq++) {
                String userId = "perf_user_" + (seq % userModulo);
                Message message = new Message(
                        topic,
                        tag,
                        sourceMsgId(perfRunId, seq),
                        messageBody(perfRunId, userId, seq).getBytes(StandardCharsets.UTF_8));
                producer.send(message);
            }
        } finally {
            producer.shutdown();
        }
    }

    static String sourceMsgId(String perfRunId, int seq) {
        return perfRunId + ":mq:" + seq;
    }

    static String messageBody(String perfRunId, String userId, int seq) {
        return "{\"userId\":\"" + escapeJson(userId) + "\","
                + "\"messageCode\":\"PERF_MQ\","
                + "\"payload\":{"
                + "\"perfRunId\":\"" + escapeJson(perfRunId) + "\","
                + "\"perfInputId\":\"" + escapeJson(sourceMsgId(perfRunId, seq)) + "\","
                + "\"seq\":" + seq
                + "}}";
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> parsedArgs = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if (!key.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + key);
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for " + key);
            }
            parsedArgs.put(key, args[++i]);
        }
        return parsedArgs;
    }

    private static String require(Map<String, String> args, String key) {
        String value = args.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private static int parseInt(String value, String key) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be an integer", e);
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
