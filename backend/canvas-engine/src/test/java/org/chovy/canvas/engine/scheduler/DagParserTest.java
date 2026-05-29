package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * DagParser 单元测试。
 * 覆盖：正常 DAG 解析、Kahn 环检测、入边统计。
 */
class DagParserTest {

    private final DagParser parser;

    DagParserTest() {
        com.fasterxml.jackson.databind.ObjectMapper om =
                new com.fasterxml.jackson.databind.ObjectMapper();
        om.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        parser = new DagParser(om);
    }

    @Test
    @DisplayName("简单链式 DAG 正常解析")
    void simple_chain_parses() {
        String json = """
            {"nodes":[
              {"id":"n1","type":"MQ_TRIGGER","name":"触发","config":{"nextNodeId":"n2"}},
              {"id":"n2","type":"IF_CONDITION","name":"判断",
               "config":{"rules":[],"successNodeId":"n3","failNodeId":null}},
              {"id":"n3","type":"COUPON","name":"发券","config":{}}
            ]}
            """;
        DagGraph g = parser.parse(json);
        assertThat(g.entryNodes()).containsExactly("n1");
        assertThat(g.downstream("n1")).containsExactly("n2");
        assertThat(g.downstream("n2")).containsExactly("n3");
    }

    @Test
    @DisplayName("Kahn 算法检测到环时抛出异常")
    void cycle_detected_throws() {
        // n1 → n2 → n1（环）
        String json = """
            {"nodes":[
              {"id":"n1","type":"MQ_TRIGGER","config":{"nextNodeId":"n2"}},
              {"id":"n2","type":"DELAY","config":{"nextNodeId":"n1"}}
            ]}
            """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("循环");
    }

    @Test
    @DisplayName("IF_CONDITION 两条出边（success/fail）均被识别")
    void if_condition_two_edges() {
        String json = """
            {"nodes":[
              {"id":"n1","type":"MQ_TRIGGER","config":{"nextNodeId":"n2"}},
              {"id":"n2","type":"IF_CONDITION",
               "config":{"successNodeId":"n3","failNodeId":"n4"}},
              {"id":"n3","type":"COUPON","config":{}},
              {"id":"n4","type":"REACH_PLATFORM","config":{}}
            ]}
            """;
        DagGraph g = parser.parse(json);
        assertThat(g.downstream("n2")).containsExactlyInAnyOrder("n3", "n4");
        // n3、n4 各有一条入边
        assertThat(g.upstream("n3")).containsExactly("n2");
        assertThat(g.upstream("n4")).containsExactly("n2");
    }

    @Test
    @DisplayName("节点运行时 UI 元数据不影响 DAG 解析")
    void outlet_schema_metadata_parses() {
        String json = """
            {"nodes":[
              {"id":"n1","type":"WAIT","name":"等待",
               "outletSchema":"[{\\\"id\\\":\\\"success\\\",\\\"label\\\":\\\"继续\\\"}]",
               "config":{"timeoutNodeId":"n2","suppressedNodeId":"n3","skippedNodeId":"n4",
                         "maxExceededNodeId":"n5","goalMetNodeId":"n6","goalNotMetNodeId":"n7"}},
              {"id":"n2","type":"END","config":{}},
              {"id":"n3","type":"END","config":{}},
              {"id":"n4","type":"END","config":{}},
              {"id":"n5","type":"END","config":{}},
              {"id":"n6","type":"END","config":{}},
              {"id":"n7","type":"END","config":{}}
            ]}
            """;

        DagGraph g = parser.parse(json);

        assertThat(g.getNode("n1").getOutletSchema()).contains("success");
        assertThat(g.downstream("n1")).containsExactlyInAnyOrder("n2", "n3", "n4", "n5", "n6", "n7");
    }

    @Test
    @DisplayName("普通副作用节点不允许直接多入边收敛")
    void normal_node_multi_input_convergence_rejected() {
        String json = """
            {"nodes":[
              {"id":"start","type":"MQ_TRIGGER","config":{"successNodeId":"a","failNodeId":"b"}},
              {"id":"a","type":"DELAY","config":{"nextNodeId":"send"}},
              {"id":"b","type":"DELAY","config":{"nextNodeId":"send"}},
              {"id":"send","type":"SEND_SMS","config":{}}
            ]}
            """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("多分支收敛")
                .hasMessageContaining("send");
    }

    @Test
    @DisplayName("显式收敛节点允许多入边")
    void convergence_nodes_allow_multi_input() {
        for (String type : java.util.List.of("HUB", "LOGIC_RELATION", "AGGREGATE", "THRESHOLD")) {
            String json = """
                {"nodes":[
                  {"id":"start","type":"MQ_TRIGGER","config":{"successNodeId":"a","failNodeId":"b"}},
                  {"id":"a","type":"DELAY","config":{"nextNodeId":"merge"}},
                  {"id":"b","type":"DELAY","config":{"nextNodeId":"merge"}},
                  {"id":"merge","type":"%s","config":{}}
                ]}
                """.formatted(type);

            assertThat(parser.parse(json).upstream("merge")).containsExactlyInAnyOrder("a", "b");
        }
    }

    @Test
    @DisplayName("END 结束节点允许多条分支直接结束")
    void end_node_allows_multi_input() {
        String json = """
            {"nodes":[
              {"id":"start","type":"MQ_TRIGGER","config":{"successNodeId":"a","failNodeId":"b"}},
              {"id":"a","type":"DELAY","config":{"nextNodeId":"end"}},
              {"id":"b","type":"DELAY","config":{"nextNodeId":"end"}},
              {"id":"end","type":"END","config":{}}
            ]}
            """;

        assertThat(parser.parse(json).upstream("end")).containsExactlyInAnyOrder("a", "b");
    }
}
