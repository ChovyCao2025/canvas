package org.chovy.canvas.perf.mq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PerfMqProducer {
    private static final String DEFAULT_NAME_SERVER = "localhost:9876";
    private static final String DEFAULT_TOPIC = "CANVAS_MQ_TRIGGER";
    private static final String DEFAULT_TAG = "PERF_MQ";
    private static final int DEFAULT_COUNT = 1000;
    private static final int DEFAULT_USER_MODULO = 1000;
    private static final Set<String> ARG_NAMES = Set.of(
            "--name-server",
            "--topic",
            "--tag",
            "--perf-run-id",
            "--count",
            "--user-modulo");

    public static void main(String[] args) throws Exception {
        Config config = Config.fromArgs(args);
        run(config, RocketMqProducerClient::new);
    }

    static int run(Config config, ProducerFactory producerFactory) throws Exception {
        if (!config.shouldSend()) {
            return 0;
        }

        ProducerClient producer = producerFactory.create(config.nameServer());
        int sent = 0;
        try {
            producer.start();
            for (int seq : sequences(config.count())) {
                String userId = "perf_user_" + (seq % config.userModulo());
                producer.send(
                        config.topic(),
                        config.tag(),
                        sourceMsgId(config.perfRunId(), seq),
                        messageBody(config.perfRunId(), userId, seq).getBytes(StandardCharsets.UTF_8));
                sent++;
            }
        } finally {
            producer.close();
        }
        return sent;
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
            if (!ARG_NAMES.contains(key)) {
                throw new IllegalArgumentException("Unknown argument: " + key);
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

    private static int parseNonNegativeInt(String value, String key) {
        int parsed = parseInt(value, key);
        if (parsed < 0) {
            throw new IllegalArgumentException(key + " must be >= 0");
        }
        return parsed;
    }

    private static int parsePositiveInt(String value, String key) {
        int parsed = parseInt(value, key);
        if (parsed <= 0) {
            throw new IllegalArgumentException(key + " must be > 0");
        }
        return parsed;
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
    }

    record Config(String nameServer, String topic, String tag, String perfRunId, int count, int userModulo) {
        static Config fromArgs(String[] args) {
            Map<String, String> parsedArgs = parseArgs(args);
            return new Config(
                    parsedArgs.getOrDefault("--name-server", DEFAULT_NAME_SERVER),
                    parsedArgs.getOrDefault("--topic", DEFAULT_TOPIC),
                    parsedArgs.getOrDefault("--tag", DEFAULT_TAG),
                    require(parsedArgs, "--perf-run-id"),
                    parseNonNegativeInt(parsedArgs.getOrDefault("--count", Integer.toString(DEFAULT_COUNT)), "--count"),
                    parsePositiveInt(parsedArgs.getOrDefault("--user-modulo", Integer.toString(DEFAULT_USER_MODULO)), "--user-modulo"));
        }

        boolean shouldSend() {
            return count > 0;
        }
    }

    interface ProducerFactory {
        ProducerClient create(String nameServer);
    }

    interface ProducerClient extends AutoCloseable {
        void start() throws Exception;

        void send(String topic, String tag, String key, byte[] body) throws Exception;

        @Override
        void close();
    }

    private static final class RocketMqProducerClient implements ProducerClient {
        private final DefaultMQProducer producer;

        private RocketMqProducerClient(String nameServer) {
            this.producer = new DefaultMQProducer("PID_CANVAS_PERF");
            this.producer.setNamesrvAddr(nameServer);
        }

        @Override
        public void start() throws Exception {
            producer.start();
        }

        @Override
        public void send(String topic, String tag, String key, byte[] body) throws Exception {
            producer.send(new Message(topic, tag, key, body));
        }

        @Override
        public void close() {
            producer.shutdown();
        }
    }
}
