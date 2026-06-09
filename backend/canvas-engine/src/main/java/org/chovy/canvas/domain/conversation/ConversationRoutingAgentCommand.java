package org.chovy.canvas.domain.conversation;

import java.util.List;
import java.util.Map;

/**
 * ConversationRoutingAgentCommand 承载 domain.conversation 场景中的不可变数据快照。
 * @param agentKey agentKey 字段。
 * @param displayName displayName 字段。
 * @param teamKey teamKey 字段。
 * @param status status 字段。
 * @param maxCapacity maxCapacity 字段。
 * @param currentLoad currentLoad 字段。
 * @param skills skills 字段。
 * @param metadata metadata 字段。
 */
public record ConversationRoutingAgentCommand(String agentKey,
                                              String displayName,
                                              String teamKey,
                                              String status,
                                              Integer maxCapacity,
                                              Integer currentLoad,
                                              List<String> skills,
                                              Map<String, Object> metadata) {

    public ConversationRoutingAgentCommand {
        skills = skills == null ? List.of() : List.copyOf(skills);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
