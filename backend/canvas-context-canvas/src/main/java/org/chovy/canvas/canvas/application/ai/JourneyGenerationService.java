package org.chovy.canvas.canvas.application.ai;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.chovy.canvas.canvas.api.ai.AiJourneyDraftProposal;

/**
 * 封装JourneyGenerationService相关的业务逻辑。
 */
public class JourneyGenerationService {

    /**
     * 保存provider。
     */
    private final JourneyDraftProvider provider;

    /**
     * 保存时钟。
     */
    private final Clock clock;

    /**
     * 创建当前对象实例。
     */
    public JourneyGenerationService(JourneyDraftProvider provider, Clock clock) {
        this.provider = Objects.requireNonNull(provider, "provider is required");
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /**
     * 处理mock。
     */
    public static JourneyGenerationService mock(Clock clock) {
        return new JourneyGenerationService(new MockJourneyDraftProvider(), clock);
    }

    /**
     * 处理generateDraft。
     */
    public AiJourneyDraftProposal generateDraft(GenerationRequest request) {
        Objects.requireNonNull(request, "request is required");
        GeneratedDraft draft = provider.generate(request);
        Instant now = Instant.now(clock);
        return new AiJourneyDraftProposal(
                request.tenantId(),
                proposalId(request, now),
                request.prompt(),
                draft.dslDraft(),
                request.riskFindings(),
                request.traceReferences(),
                now);
    }

    /**
     * 处理proposal标识。
     */
    private static String proposalId(GenerationRequest request, Instant createdAt) {
        String slug = slug(request.prompt().isBlank() ? "marketing journey" : request.prompt());
        return "ai-journey-" + slug + "-" + createdAt.toEpochMilli();
    }

    /**
     * 处理slug。
     */
    private static String slug(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            return "draft";
        }
        return normalized.length() <= 48 ? normalized : normalized.substring(0, 48).replaceAll("-$", "");
    }

    /**
     * 定义JourneyDraftProvider对外提供的能力契约。
     */
    public interface JourneyDraftProvider {

        /**
         * 处理generate。
         */
        GeneratedDraft generate(GenerationRequest request);
    }

    /**
     * 承载GenerationRequest的数据快照。
     */
    public record GenerationRequest(
            /**
             * 记录租户标识。
             */
            Long tenantId,
            /**
             * 记录prompt。
             */
            String prompt,
            /**
             * 记录riskFindings。
             */
            List<AiJourneyDraftProposal.RiskFinding> riskFindings,
            /**
             * 记录traceReferences。
             */
            List<AiJourneyDraftProposal.TraceReference> traceReferences,
            /**
             * 记录操作人。
             */
            String operator) {

        public GenerationRequest {
            if (tenantId == null || tenantId <= 0) {
                throw new IllegalArgumentException("tenantId is required");
            }
            prompt = prompt == null || prompt.isBlank() ? "" : prompt.trim();
            riskFindings = List.copyOf(riskFindings == null ? List.of() : riskFindings);
            traceReferences = List.copyOf(traceReferences == null ? List.of() : traceReferences);
            operator = operator == null ? "" : operator.trim();
        }
    }

    /**
     * 承载GeneratedDraft的数据快照。
     */
    public record GeneratedDraft(String dslDraft) {

        public GeneratedDraft {
            if (dslDraft == null || dslDraft.isBlank()) {
                throw new IllegalArgumentException("dslDraft is required");
            }
        }
    }

    /**
     * 封装MockJourneyDraftProvider相关的业务逻辑。
     */
    private static final class MockJourneyDraftProvider implements JourneyDraftProvider {

        /**
         * 处理generate。
         */
        @Override
        public GeneratedDraft generate(GenerationRequest request) {
            String prompt = request.prompt().toLowerCase(Locale.ROOT);
            if (prompt.contains("new user") || prompt.contains("welcome")) {
                return new GeneratedDraft("""
                        apiVersion: canvas/v1
                        kind: Journey
                        metadata:
                          name: ai-new-user-welcome
                          title: AI New User Welcome
                        spec:
                          trigger:
                            type: webhook
                            event: user.registered
                          nodes:
                            - id: segment
                              type: condition
                              config:
                                expression: "user.lifecycle == 'new'"
                            - id: risk
                              type: risk-check
                              config:
                                mode: preview
                            - id: approval
                              type: approval
                              config:
                                approverRole: campaign-owner
                            - id: message
                              type: message
                              config:
                                channel: sms
                                template: welcome
                            - id: coupon
                              type: coupon
                              config:
                                maxRedemptions: 1000
                            - id: end
                              type: end
                              config: {}
                          edges:
                            - from: segment
                              to: risk
                            - from: risk
                              to: approval
                            - from: approval
                              to: message
                            - from: message
                              to: coupon
                            - from: coupon
                              to: end
                        """);
            }

            return new GeneratedDraft("""
                    apiVersion: canvas/v1
                    kind: Journey
                    metadata:
                      name: ai-marketing-journey
                      title: AI Marketing Journey
                    spec:
                      trigger:
                        type: webhook
                        event: demo.event
                      nodes:
                        - id: risk
                          type: risk-check
                          config:
                            mode: preview
                        - id: message
                          type: message
                          config:
                            channel: sms
                            frequencyCap: "1/day"
                        - id: end
                          type: end
                          config: {}
                      edges:
                        - from: risk
                          to: message
                        - from: message
                          to: end
                    """);
        }
    }
}
