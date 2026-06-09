package org.chovy.canvas.perf.mq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * PerfMqProducer 封装本模块的核心职责、输入输出结构和协作边界。
 */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param args 命令行参数，用于读取运行配置。
     */
    public static void main(String[] args) throws Exception {
        Config config = Config.fromArgs(args);
        run(config, RocketMqProducerClient::new);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param config 配置对象，用于控制运行参数和策略开关。
     * @param producerFactory 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @return 返回流程执行后的业务结果。
     */
    static int run(Config config, ProducerFactory producerFactory) throws Exception {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!config.shouldSend()) {
            return 0;
        }

        ProducerClient producer = producerFactory.create(config.nameServer());
        int sent = 0;
        try {
            producer.start();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return sent;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param perfRunId 业务对象 ID，用于定位具体记录。
     * @param seq 分页、数量或序号参数，用于控制处理规模。
     * @return 返回 source msg id 生成的文本或业务键。
     */
    static String sourceMsgId(String perfRunId, int seq) {
        return perfRunId + ":mq:" + seq;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param perfRunId 业务对象 ID，用于定位具体记录。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param seq 分页、数量或序号参数，用于控制处理规模。
     * @return 返回 message body 生成的文本或业务键。
     */
    static String messageBody(String perfRunId, String userId, int seq) {
        return "{\"userId\":\"" + escapeJson(userId) + "\","
                + "\"messageCode\":\"PERF_MQ\","
                + "\"payload\":{"
                + "\"perfRunId\":\"" + escapeJson(perfRunId) + "\","
                + "\"perfInputId\":\"" + escapeJson(sourceMsgId(perfRunId, seq)) + "\","
                + "\"seq\":" + seq
                + "}}";
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param count 分页、数量或序号参数，用于控制处理规模。
     * @return 返回 sequences 计算得到的数量、金额或指标值。
     */
    static int[] sequences(int count) {
        int[] sequences = new int[count];
        for (int i = 0; i < count; i++) {
            sequences[i] = i + 1;
        }
        return sequences;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param args 命令行参数，用于读取运行配置。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> parsedArgs = new HashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return parsedArgs;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param MapString map string 参数，用于 require 流程中的校验、计算或对象转换。
     * @param args 命令行参数，用于读取运行配置。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 require 生成的文本或业务键。
     */
    private static String require(Map<String, String> args, String key) {
        String value = args.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int parseInt(String value, String key) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be an integer", e);
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int parseNonNegativeInt(String value, String key) {
        int parsed = parseInt(value, key);
        if (parsed < 0) {
            throw new IllegalArgumentException(key + " must be >= 0");
        }
        return parsed;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int parsePositiveInt(String value, String key) {
        int parsed = parseInt(value, key);
        if (parsed <= 0) {
            throw new IllegalArgumentException(key + " must be > 0");
        }
        return parsed;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
                    // 校验关键输入和前置条件，避免无效状态继续进入主流程。
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return escaped.toString();
    }

    /**
     * Config 封装本模块的核心职责、输入输出结构和协作边界。
     */
    record Config(String nameServer, String topic, String tag, String perfRunId, int count, int userModulo) {
        /**
         * 组装输出结构或完成对象转换。
         *
         * @param args 命令行参数，用于读取运行配置。
         * @return 返回组装或转换后的结果对象。
         */
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

        /**
         * 校验输入、权限或业务前置条件。
         *
         * @return 返回布尔判断结果。
         */
        boolean shouldSend() {
            return count > 0;
        }
    }

    /**
     * ProducerFactory 封装本模块的核心职责、输入输出结构和协作边界。
     */
    interface ProducerFactory {
        /**
         * 创建业务对象并完成必要的初始化。
         *
         * @param nameServer name server 参数，用于 create 流程中的校验、计算或对象转换。
         * @return 返回流程执行后的业务结果。
         */
        ProducerClient create(String nameServer);
    }

    /**
     * ProducerClient 封装本模块的核心职责、输入输出结构和协作边界。
     */
    interface ProducerClient extends AutoCloseable {
        /**
         * 根据方法职责完成对应的业务处理流程。
         */
        void start() throws Exception;

        /**
         * 执行核心业务流程，并协调依赖组件完成处理。
         *
         * @param topic 待处理业务值，用于规则计算、转换或外部调用。
         * @param tag 待处理业务值，用于规则计算、转换或外部调用。
         * @param key 业务键，用于在同一租户下定位资源。
         * @param body 待处理业务值，用于规则计算、转换或外部调用。
         */
        void send(String topic, String tag, String key, byte[] body) throws Exception;

        @Override
        /**
         * 清理、停用或释放指定业务资源。
         */
        void close();
    }

    /**
     * RocketMqProducerClient 封装本模块的核心职责、输入输出结构和协作边界。
     */
    private static final class RocketMqProducerClient implements ProducerClient {
        private final DefaultMQProducer producer;

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param nameServer name server 参数，用于 RocketMqProducerClient 流程中的校验、计算或对象转换。
         * @return 返回 RocketMqProducerClient 流程生成的业务结果。
         */
        private RocketMqProducerClient(String nameServer) {
            this.producer = new DefaultMQProducer("PID_CANVAS_PERF");
            this.producer.setNamesrvAddr(nameServer);
        }

        @Override
        /**
         * 根据方法职责完成对应的业务处理流程。
         */
        public void start() throws Exception {
            producer.start();
        }

        @Override
        /**
         * 执行核心业务流程，并协调依赖组件完成处理。
         *
         * @param topic 待处理业务值，用于规则计算、转换或外部调用。
         * @param tag 待处理业务值，用于规则计算、转换或外部调用。
         * @param key 业务键，用于在同一租户下定位资源。
         * @param body 待处理业务值，用于规则计算、转换或外部调用。
         */
        public void send(String topic, String tag, String key, byte[] body) throws Exception {
            producer.send(new Message(topic, tag, key, body));
        }

        @Override
        /**
         * 清理、停用或释放指定业务资源。
         */
        public void close() {
            producer.shutdown();
        }
    }
}
