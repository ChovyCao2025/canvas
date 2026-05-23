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
        Config config = Config.fromArgs(args);
        if (!config.shouldSend()) {
            return;
        }

        DefaultMQProducer producer = new DefaultMQProducer("PID_CANVAS_PERF");
        producer.setNamesrvAddr(config.nameServer());
        try {
            producer.start();
            for (int seq : sequences(config.count())) {
                String userId = "perf_user_" + (seq % config.userModulo());
                Message message = new Message(
                        config.topic(),
                        config.tag(),
                        sourceMsgId(config.perfRunId(), seq),
                        messageBody(config.perfRunId(), userId, seq).getBytes(StandardCharsets.UTF_8));
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

    static int[] sequences(int count) {
        int[] sequences = new int[count];
        for (int i = 0; i < count; i++) {
            sequences[i] = i + 1;
        }
        return sequences;
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

    record Config(String nameServer, String topic, String tag, String perfRunId, int count, int userModulo) {
        static Config fromArgs(String[] args) {
            Map<String, String> parsedArgs = parseArgs(args);
            return new Config(
                    parsedArgs.getOrDefault("--name-server", DEFAULT_NAME_SERVER),
                    parsedArgs.getOrDefault("--topic", DEFAULT_TOPIC),
                    parsedArgs.getOrDefault("--tag", DEFAULT_TAG),
                    require(parsedArgs, "--perf-run-id"),
                    parseInt(parsedArgs.getOrDefault("--count", Integer.toString(DEFAULT_COUNT)), "--count"),
                    parseInt(parsedArgs.getOrDefault("--user-modulo", Integer.toString(DEFAULT_USER_MODULO)), "--user-modulo"));
        }

        boolean shouldSend() {
            return count > 0;
        }
    }
}
