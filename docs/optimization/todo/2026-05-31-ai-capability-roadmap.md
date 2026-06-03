# AI Capability Catch-up Roadmap: Marketing Canvas Platform

> Version: 1.0 | Date: 2026-05-31
> Scope: 3-phase AI capability roadmap for China B2C SMB marketing platform
> Current AI Coverage: <2% (1 stub node: AI_NEXT_BEST_ACTION)
> Baseline Assets: AI_LLM node design doc (multi-provider, prompt templates), GroovyHandler, SCORING (rule-based), RECOMMENDATION (stub), API_CALL (external HTTP)

---

## 0. Strategic Assessment

### 0.1 Which Competitor's AI Approach Is Most Relevant?

**Answer: Klaviyo's phased approach, not Customer.io's Agent approach.**

| Competitor | AI Approach | Relevance to China B2C SMB | Verdict |
|-----------|-------------|---------------------------|---------|
| **Klaviyo** | Predictive models (churn/CLV/next order/channel affinity) + Flows AI (NL→canvas) + Strategy Agent | **HIGH** — Predictive analytics matches what China B2C SMB operators actually need (churn, CLV, next purchase). Phased rollout from "AI assists" to "AI decides" fits resource constraints. | **Primary reference** |
| Braze | Intelligent Timing + AI copywriting + predictive churn/affinity | MEDIUM — Good feature-level reference, but enterprise pricing ($100K+/yr) and lack of China ecosystem make positioning irrelevant | Feature-level copy |
| Customer.io | Cowork AI Agent (MCP + LLM Actions + Routines) | LOW — Most disruptive but 6-12 months ahead. Requires mature event infrastructure, plugin system, and real channel connectors — all currently missing. Premature for a platform at <2% AI coverage. | Aspirational only |
| Iterable | AI Suite (brand affinity, predictive goals, STO) | MEDIUM — Good developer-friendly API patterns, but feature set overlaps with Klaviyo without the ecommerce depth | Secondary reference |
| CleverTap | Scribe AI (content) + predictive analytics | LOW-MEDIUM — India/SE Asia focus, similar SMB segment but weaker AI execution than Klaviyo | Marginal |

**Why Klaviyo, not Customer.io:** Customer.io's Cowork Agent requires (1) a plugin system (this platform has none — new nodes need Java code + restart), (2) real channel connectors (all point to WireMock), (3) a mature event pipeline with guaranteed delivery, and (4) a structured action/routine model. Building an AI Agent on top of a platform missing these foundations is putting the cart before the horse. Klaviyo's approach — start with predictive values that flow into existing canvas nodes — maps directly onto this platform's existing architecture (ScoringHandler + IfCondition + Selector).

### 0.2 Minimum Viable AI That Changes Market Positioning

**One capability: AI-powered churn prediction that feeds directly into canvas routing.**

Why this specifically:
1. **Solves the #1 operator pain point** — "I don't know which users are about to leave until they're already gone"
2. **Fits the existing architecture** — churn_probability is a number that flows into ScoringHandler bands → IfCondition routes → different retention treatments. No new routing primitives needed.
3. **Differentiates in China market** — no China-native platform offers per-user churn probability today. ShenCe has analytics dashboards but not real-time canvas-integrated prediction. Volcengine has the compute but not the canvas integration.
4. **Provable ROI** — "reduced 30-day churn by X%" is the most compelling SaaS metric for renewal/expansion

Minimum viable implementation:
- Batch job: compute `churn_probability` (0-1) for each user daily → write to CDP profile as `churn_probability` field
- Canvas integration: IF_CONDITION reads `${churn_probability}` or SCORING includes it as a rule dimension
- One template canvas: "Churn Rescue" — if churn_probability > 0.7 → SendPush with personalized offer; if 0.4-0.7 → SendSms gentle nudge; if < 0.4 → skip
- Dashboard: churn risk distribution + intervention effectiveness tracking

### 0.3 AI Capabilities That Should NOT Be Built (Commoditized / Better to Integrate)

| Capability | Reason | What to Do Instead |
|-----------|--------|-------------------|
| **LLM hosting/fine-tuning** | Commodity — DeepSeek/Qwen/Moonshot APIs are cheap and improving rapidly. Building your own model is a 10x cost multiplier with zero moat. | Use existing AI_LLM node's multi-provider architecture. Default to DeepSeek for cost, Qwen for compliance. |
| **AI copywriting assistant** | Commodity — every platform has this. OpenAI/DeepSeek generate marketing copy with zero differentiation. | Ship via AI_LLM node's `text_generate` template. Do not build a dedicated UI. |
| **Image generation** | Entirely commoditized — Midjourney/Stable Diffusion APIs. Zero connection to canvas execution. | Do not build. Users integrate via API_CALL if needed. |
| **Chatbot / conversational AI** | Outside the canvas execution model. Conversational AI is a different product category. | Do not build. |
| **Custom ML training pipeline** | Premature — the platform doesn't have enough labeled training data (no conversion tracking, no attribution). Building the pipeline before the data is cart-before-horse. | Use pre-trained models + zero-shot/few-shot prompting via AI_LLM node. Build training pipeline only after Phase 2 attribution is live. |
| **Natural language canvas creation** (Klaviyo Flows AI style) | Seductive but high-risk — generating valid canvas JSON requires deep understanding of all 60+ node types, config schemas, and DAG constraints. One wrong node config = broken canvas. Defer until templates and validation are mature. | Phase 3 aspiration only. |

---

## 1. Phase 1: AI-Assisted (0-6 months)

**Principle:** AI assists human operators. Every AI output is a suggestion, not a decision. Operators see what AI recommends and choose whether to follow it.

### 1.1 Specific Capabilities

| # | Capability | Description | Existing Foundation |
|---|-----------|-------------|-------------------|
| 1.1 | **AI_LLM Node (Production)** | Replace AI_NEXT_BEST_ACTION stub with real LLM call. Multi-provider (DeepSeek/Qwen/Moonshot/OpenAI), prompt template library, structured JSON output, fallback on failure. | Design doc complete, implementation plan written (8 tasks), Flyway V89 ready |
| 1.2 | **Churn Probability Prediction** | Daily batch job computes per-user churn_probability (0-1) using logistic regression on CDP behavioral features. Writes to user profile field. Canvas reads via `${churn_probability}`. | ScoringHandler + IfCondition already support numeric field routing |
| 1.3 | **AI Smart Timing** | Replace fixed DelayHandler with "smart delay" — delay until user's most active hour. Computes best_send_hour per user from event_log history. New node type or AI_LLM template. | DelayHandler exists; AI_LLM `timing` template in design doc |
| 1.4 | **AI Content Personalization** | AI_LLM `text_generate` template generates personalized SMS/Push/email copy based on user profile + context variables. Downstream SendXxx nodes consume `${ai_output.text}`. | AI_LLM design doc; SendSms/SendPush/SendEmail handlers exist |
| 1.5 | **AI-Assisted Scoring** | AI_LLM `scoring` template augments rule-based ScoringHandler. LLM evaluates unstructured signals (recent browse behavior, support ticket sentiment) that rules can't capture. Score merged with rule-based score. | ScoringHandler with rule-based bands; AI_LLM scoring template |
| 1.6 | **Churn Risk Dashboard** | New dashboard: churn risk distribution (high/medium/low segments), top-at-risk users, intervention history, 30-day churn trend. | execution_trace data available; React + Recharts already in stack |
| 1.7 | **AI Usage Analytics** | Track AI_LLM node calls: per-template invocation count, latency p50/p95, fallback rate, token consumption, cost estimation. Essential for cost control and trust building. | New telemetry; can reuse existing canvas_execution_trace structure |
| 1.8 | **Prompt Template Marketplace** | Curated library of prompt templates beyond the 4 built-in ones. Admins can import community templates. Templates versioned and tested. | ai_prompt_template table designed; CRUD API planned |

### 1.2 Technical Implementation Approach

**Models:**
- Primary: DeepSeek V3 (cost-effective, China-hosted, PIPL-compliant)
- Secondary: Qwen 2.5 via DashScope (Alibaba Cloud, enterprise compliance)
- Fallback: OpenAI GPT-4o-mini (for international customers)
- Churn prediction: Scikit-learn logistic regression (batch, not real-time) — simpler than deep learning, easier to explain, sufficient for Phase 1

**Architecture:**
```
                    ┌─────────────────────────────┐
                    │     AI Service Layer         │
                    │  ┌────────┐  ┌────────────┐  │
                    │  │AiLlm   │  │ AiPredict  │  │
                    │  │Gateway │  │ Service    │  │
                    │  │(sync)  │  │ (batch)    │  │
                    │  └───┬────┘  └─────┬──────┘  │
                    │      │             │         │
                    │  ┌───▼─────────────▼──────┐  │
                    │  │   LlmClient (strategy)  │  │
                    │  │   OpenAI | Qwen | Ollama│  │
                    │  └───┬─────────────┬──────┘  │
                    └──────┼─────────────┼─────────┘
                           │             │
                    ┌──────▼──┐    ┌─────▼──────┐
                    │ DeepSeek │    │  Qwen API  │
                    │   API    │    │ (DashScope)│
                    └──────────┘    └────────────┘
```

- **AiLlmGateway**: Already designed. Sync call within canvas execution. Timeout 30s, retry 2x, fallback to schema defaults.
- **AiPredictService**: New. Batch job (scheduled via XXL-Job or existing SCHEDULED_TRIGGER). Reads CDP user features from MySQL, runs sklearn model, writes `churn_probability` back to user profile. Runs daily at 02:00.
- **Smart Timing**: Compute `best_send_hour` per user from event_log. Simple aggregation: `HOUR(event_time) GROUP BY user_id ORDER BY COUNT(*) DESC LIMIT 1`. Store in CDP profile. AI_LLM timing template provides refinement.

**Data Requirements:**
- Churn model: 90+ days of event history, at least 1000 users with known churn outcome for training. Features: days since last purchase, purchase frequency, average order value, last active date, support ticket count, email open rate.
- Smart timing: 30+ days of event_log per user with timestamps.
- Content personalization: User profile fields (age, gender, location, preferences) + canvas context variables (product info, offer details).

### 1.3 User-Facing Features

| Feature | What the Marketer Sees |
|---------|----------------------|
| AI_LLM node in canvas | Drag "AI Smart Node" onto canvas. Select template (Copywriting / Smart Scoring / Smart Timing / Product Recommendation / Custom). Configure model and parameters. Template auto-fills prompt and output schema. |
| Churn risk in canvas | IF_CONDITION node: `${churn_probability} > 0.7` → high-risk branch. ScoringHandler: include churn_probability as a scoring dimension. |
| Churn dashboard | New "AI Analytics" page: churn risk pie chart, top-100 at-risk users table, 30-day trend, intervention success rate. |
| Smart timing in canvas | DelayHandler: select "Smart Delay" instead of "Fixed Delay" → delay until user's best hour. Or use AI_LLM timing template for LLM-refined prediction. |
| AI copywriting | AI_LLM node → "Copywriting" template → input product info + user profile → output `{ text, subject, tone }` → downstream SendSms uses `${ai_output.text}`. |
| AI usage metrics | Admin page: AI call volume by template, avg latency, fallback rate, monthly cost estimate. Per-canvas AI cost attribution. |

### 1.4 Dependencies

| Dependency | Status | Impact If Missing |
|-----------|--------|------------------|
| AI_LLM node implementation | Plan written (8 tasks), not yet executed | BLOCKS 1.1, 1.3, 1.4, 1.5, 1.8 |
| ai_provider + ai_prompt_template tables | Designed in V89 migration, not yet applied | BLOCKS 1.1 |
| CDP user profile fields (churn_probability, best_send_hour) | Schema supports it, no code to compute | BLOCKS 1.2, 1.3 |
| event_log with enough history | Exists but may be sparse in new deployments | BLOCKS 1.2, 1.3 |
| Attributed conversion tracking | MISSING (no attribution in platform) | WEAKENS 1.2 — churn model accuracy degrades without conversion signals |
| Admin API for AI provider/template CRUD | Designed in implementation plan, not yet built | BLOCKS 1.1, 1.8 |
| Frontend AI_LLM config panel | Designed in implementation plan, not yet built | BLOCKS 1.1 |

### 1.5 Effort Estimate

| Capability | Effort (person-months) | Breakdown |
|-----------|----------------------|-----------|
| 1.1 AI_LLM Node (Production) | 2.0 | Backend: 1.0 (handler + gateway + clients + migration + tests), Frontend: 0.5 (config panel + constants), Integration: 0.5 |
| 1.2 Churn Probability Prediction | 1.5 | Model: 0.5 (feature engineering + training + evaluation), Service: 0.5 (batch job + profile writer), Dashboard: 0.5 |
| 1.3 AI Smart Timing | 1.0 | Aggregation job: 0.5, AI_LLM template integration: 0.3, Testing: 0.2 |
| 1.4 AI Content Personalization | 0.5 | Mostly configuration of AI_LLM text_generate template + testing with real channels |
| 1.5 AI-Assisted Scoring | 0.5 | AI_LLM scoring template + score merging logic + testing |
| 1.6 Churn Risk Dashboard | 1.0 | Backend API: 0.3, Frontend charts: 0.5, Testing: 0.2 |
| 1.7 AI Usage Analytics | 0.8 | Telemetry collection: 0.3, Backend aggregation: 0.3, Frontend: 0.2 |
| 1.8 Prompt Template Marketplace | 0.7 | Import/export: 0.3, Versioning: 0.2, UI: 0.2 |
| **Phase 1 Total** | **8.0** | ~4 months with 2 people, or ~2.5 months with 3 people |

### 1.6 Success Metrics

| Metric | Target (6-month) | How to Measure |
|--------|-----------------|----------------|
| AI_LLM node adoption | 20%+ of active canvases contain at least 1 AI_LLM node | Canvas graph JSON scan |
| Churn model AUC | > 0.75 on holdout test set | Standard ML evaluation |
| Churn prediction usage | 10%+ of active canvases reference churn_probability | Canvas graph JSON scan |
| AI copywriting usage | 500+ AI_LLM text_generate calls/week | ai_usage_metrics table |
| AI fallback rate | < 5% of AI_LLM calls use fallback values | ai_usage_metrics table |
| AI p95 latency | < 10 seconds for AI_LLM node execution | execution_trace timing |
| Operator NPS for AI features | > 30 (net promoter score on AI features) | Quarterly survey |
| Churn intervention lift | 15%+ lower 30-day churn in AI-targeted segment vs control | A/B experiment within canvas |

---

## 2. Phase 2: AI-Automated (6-18 months)

**Principle:** AI makes some decisions autonomously with human oversight. Operators set guardrails; AI operates within them. Operators review AI decisions after the fact, not before.

### 2.1 Specific Capabilities

| # | Capability | Description | Phase 1 Foundation |
|---|-----------|-------------|-------------------|
| 2.1 | **Predicted CLV (Customer Lifetime Value)** | Batch computation of predicted_clv per user. Enables value-based routing (VIP → exclusive offers, low-value → cost-efficient channels). | Extends churn prediction pipeline |
| 2.2 | **Predicted Next Purchase Date** | Per-user expected_date_of_next_order. Enables proactive campaigns ("buy again before you forget"). Klaviyo's most impactful prediction. | Extends prediction service with new model |
| 2.3 | **Channel Affinity Prediction** | Per-user ranked channel preference (e.g., ["push", "sms", "email"]). Auto-selects best channel in canvas. | New prediction model; integrates with existing channel routing |
| 2.4 | **AI-Optimized Send Time** | Move from "best hour" to full send-time optimization: considers channel, message type, user timezone, and recent send history. Per-node per-user dynamic timing. | Upgrades Phase 1 smart timing with multi-signal model |
| 2.5 | **Attribution-Aware AI Scoring** | AI scoring that incorporates attribution data (which touchpoints contributed to conversion). Makes scoring causally informed, not just correlational. | REQUIRES attribution system (P0 gap item) to be built first |
| 2.6 | **AI Fatigue Management** | AI determines per-user fatigue threshold based on engagement patterns (not just fixed frequency cap). Auto-suppresses users showing engagement decline. | Extends FrequencyCapHandler from fixed rules to AI-determined thresholds |
| 2.7 | **Canvas Performance Anomaly Detection** | AI monitors canvas execution metrics (success rate, conversion rate, delivery rate) and auto-alerts when performance degrades. Suggests root causes. | Builds on AI usage analytics from Phase 1 |
| 2.8 | **Automated A/B Winner Promotion** | When an A/B experiment reaches statistical significance, AI auto-promotes the winning variant. Operator reviews after the fact. | REQUIRES experiment statistical engine (gap item) |

### 2.2 Technical Implementation Approach

**Models:**
- CLV: BG/NBD + Gamma-Gamma model (probabilistic, well-understood for ecommerce) or XGBoost with feature engineering
- Next Purchase Date: Poisson process model on inter-purchase intervals, or survival analysis (Cox PH)
- Channel Affinity: Multiclass classifier on user-channel engagement history. Features: per-channel open rate, click rate, response time, opt-out events.
- Send Time Optimization: Bayesian optimization over (hour-of-day, day-of-week, channel) space per user. Requires 4+ weeks of send history per user.
- Fatigue Model: Binary classifier predicting "will disengage in next 7 days" based on recent send frequency, channel mix, and engagement decline rate.
- Anomaly Detection: Statistical process control (3-sigma) on rolling canvas metrics + Isolation Forest for multivariate anomalies.

**Architecture Evolution:**
```
Phase 1:                              Phase 2:
┌──────────────────┐        ┌────────────────────────────┐
│  Canvas Engine   │        │     Canvas Engine          │
│  + AI_LLM Node   │  --->  │  + AI_LLM Node             │
│  + Batch Predict │        │  + Predict Service (4 models)│
│                  │        │  + Auto-Decision Layer      │
│  Operator        │        │  + Attribution Integration  │
│  decides all     │        │                             │
│                  │        │  AI decides within           │
│                  │        │  guardrails; operator        │
│                  │        │  reviews after the fact      │
└──────────────────┘        └────────────────────────────┘
```

New component: **AiDecisionLayer** — sits between prediction outputs and canvas routing. Applies guardrails:
- Min/max thresholds (e.g., churn_probability > 0.8 required for auto-suppression)
- Budget constraints (don't send expensive channel to low-CLV users)
- Fatigue caps (AI can't exceed configured max-frequency)
- Human approval required for decisions affecting > N users per day

**Data Requirements:**
- CLV model: 180+ days of purchase history, at least 5000 users with known LTV outcome
- Channel affinity: 60+ days of per-channel engagement data (opens, clicks, conversions per channel per user)
- Attribution: Conversion tracking with multi-touch attribution (first-touch, last-touch, linear) — THIS IS THE GATEKEEPER DEPENDENCY
- A/B statistical engine: Experiment results with per-variant conversion counts, confidence intervals, p-values

### 2.3 User-Facing Features

| Feature | What the Marketer Sees |
|---------|----------------------|
| Predicted CLV in canvas | New profile fields: `${predicted_clv}`, `${clv_band}` (high/medium/low). ScoringHandler and IfCondition can route on these. Dashboard: CLV distribution, CLV trend, CLV by acquisition channel. |
| Next purchase prediction | IF_CONDITION: `${days_until_next_purchase} < 7` → send replenishment reminder. Template canvas: "Purchase Prediction Campaign" — auto-targets users likely to buy soon. |
| Channel affinity routing | Selector node: "Smart Channel" option — AI picks best channel per user. Or IF_CONDITION: `${preferred_channel} == 'sms'` → SendSms branch. |
| AI fatigue management | FrequencyCapHandler: "AI Mode" toggle — instead of fixed limits, AI sets per-user fatigue thresholds. Dashboard: fatigue prediction accuracy, auto-suppression count. |
| Anomaly alerts | Notification (Phase 1 gap: need email/webhook notifications) when canvas metrics degrade. Alert includes suspected root cause: "API_CALL node X failing 40% — circuit breaker opened?" |
| Auto A/B promotion | ExperimentHandler: when confidence > 95%, auto-promote winning variant. Audit trail: who/what/when promoted. Operator can override retroactively within 24 hours. |

### 2.4 Dependencies

| Dependency | Status | Impact If Missing |
|-----------|--------|------------------|
| Attribution system | MISSING (P0 gap) | BLOCKS 2.5 — AI scoring without attribution is correlational noise |
| Conversion tracking | MISSING (no message receipt tracking) | BLOCKS 2.5, 2.8 — can't measure what you can't track |
| Experiment statistical engine | MISSING (AB_SPLIT node exists but no significance testing) | BLOCKS 2.8 |
| Notification channels (email/webhook) | MISSING (P0 gap — only in-app) | BLOCKS 2.7 — anomaly alerts can't reach operators |
| Phase 1 churn prediction | Not yet built | BLOCKS 2.1, 2.2, 2.3 — prediction pipeline must exist first |
| 90+ days of production event data | Not yet accumulated | BLOCKS 2.1, 2.2 — model training requires data |
| Per-channel engagement tracking | MISSING (ReachDeliveryService is fire-and-forget) | BLOCKS 2.3, 2.4, 2.6 |
| Real channel connectors | ALL MOCK (WireMock) | BLOCKS 2.4, 2.6 — can't measure engagement without real sends |

### 2.5 Effort Estimate

| Capability | Effort (person-months) | Breakdown |
|-----------|----------------------|-----------|
| 2.1 Predicted CLV | 1.5 | Model: 0.5, Service: 0.5, Dashboard: 0.5 |
| 2.2 Predicted Next Purchase | 1.0 | Model: 0.4, Service: 0.3, Integration: 0.3 |
| 2.3 Channel Affinity | 1.5 | Model: 0.5, Service: 0.5, Canvas routing: 0.5 |
| 2.4 AI-Optimized Send Time | 1.5 | Model: 0.5, Dynamic delay service: 0.5, Integration: 0.5 |
| 2.5 Attribution-Aware AI Scoring | 2.0 | Attribution system (prereq): 1.5, AI integration: 0.5 |
| 2.6 AI Fatigue Management | 1.5 | Model: 0.5, FrequencyCapHandler upgrade: 0.5, Testing: 0.5 |
| 2.7 Canvas Performance Anomaly Detection | 1.0 | Anomaly detection: 0.5, Alert integration: 0.3, Dashboard: 0.2 |
| 2.8 Automated A/B Winner Promotion | 1.5 | Statistical engine: 0.8, Auto-promotion logic: 0.4, UI: 0.3 |
| **Phase 2 Total** | **11.5** | ~6 months with 2 people, or ~4 months with 3 people |

**Note:** Does not include the attribution system prerequisite (estimated 3-4 person-months). If attribution must be built first, add 3.5 person-months to the total.

### 2.6 Success Metrics

| Metric | Target (18-month) | How to Measure |
|--------|-----------------|----------------|
| Prediction model count | 4 models in production (churn, CLV, next purchase, channel affinity) | Model registry |
| Prediction coverage | 60%+ of active users have all 4 prediction values | CDP profile scan |
| AI-automated decisions/week | 10,000+ (across all canvases) | Decision audit log |
| Auto-decision override rate | < 5% of AI decisions overridden by operators | Decision audit log |
| CLV model accuracy | MAPE < 25% on holdout set | Standard ML evaluation |
| Channel affinity lift | 15%+ higher open rate on AI-selected channel vs default | A/B experiment |
| A/B auto-promotion accuracy | 95%+ of auto-promoted variants remain the winner after 7 more days | Retrospective analysis |
| Attribution coverage | 50%+ of conversions attributed to a canvas touchpoint | Attribution report |

---

## 3. Phase 3: AI Agent (18-36 months)

**Principle:** AI operates semi-autonomously, orchestrating marketing strategies. Operators define goals and constraints; AI creates and manages canvases to achieve them.

### 3.1 Specific Capabilities

| # | Capability | Description | Phase 2 Foundation |
|---|-----------|-------------|-------------------|
| 3.1 | **Goal-Driven Campaign Orchestration** | Operator specifies a goal ("reduce 30-day churn by 10%") and constraints (budget, channels, fatigue limits). AI Agent creates and manages multiple canvases to achieve the goal. Monitors progress and adapts. | Requires all 4 prediction models + attribution + fatigue management |
| 3.2 | **Natural Language Canvas Builder** | Operator describes desired campaign in natural language ("send a 10% discount to users who haven't purchased in 30 days, use Push first then SMS after 2 days if no open"). AI generates valid canvas JSON. | Requires mature template/validation system; 60+ node type schemas |
| 3.3 | **Cross-Canvas Optimization Agent** | AI resolves conflicts across active canvases (same user targeted by 3 canvases → picks best one). Optimizes global message frequency and channel allocation across all canvases. | Requires cross-canvas awareness (currently missing) + AI fatigue management |
| 3.4 | **Self-Improving Prediction Models** | Models automatically retrain when prediction accuracy degrades. A/B test model improvements against current production model. Gradual rollout of model updates. | Requires Phase 2 prediction pipeline + experiment engine |
| 3.5 | **AI Content A/B Optimization** | AI generates multiple content variants, tests them, and automatically converges on the best performing variant per user segment. Moves beyond "winning variant for everyone" to "winning variant per segment." | Requires AI copywriting + A/B auto-promotion + per-segment analytics |
| 3.6 | **Conversational Canvas Debugging** | Operator asks "why did user X not receive the Push?" AI traces the execution path, identifies the node that filtered/suppressed/failed, and explains in natural language. | Requires execution trace search + AI reasoning over trace data |
| 3.7 | **Proactive Strategy Recommendations** | AI monitors data patterns and proactively suggests campaigns: "Users who bought Product A have 40% higher churn rate — consider a retention campaign." Not operator-initiated; AI-initiated. | Requires prediction models + anomaly detection + attribution |

### 3.2 Technical Implementation Approach

**Core Architecture: AI Agent Loop**

```
┌─────────────────────────────────────────────────────┐
│                    AI Agent Core                     │
│                                                     │
│  ┌──────────┐   ┌───────────┐   ┌──────────────┐   │
│  │  Observe  │──▶│  Reason   │──▶│    Act       │   │
│  │          │   │           │   │              │   │
│  │ - Metrics│   │ - LLM     │   │ - Create     │   │
│  │ - Traces │   │   planning│   │   canvas     │   │
│  │ - Models │   │ - Goal    │   │ - Modify     │   │
│  │ - Alerts │   │   decomp  │   │   canvas     │   │
│  │          │   │ - What-if │   │ - Pause/     │   │
│  │          │   │   sim     │   │   resume     │   │
│  └──────────┘   └───────────┘   └──────────────┘   │
│       ▲                               │             │
│       │          ┌───────────┐        │             │
│       └──────────│  Evaluate │◀───────┘             │
│                  │           │                      │
│                  │ - Goal    │                      │
│                  │   progress│                      │
│                  │ - ROI     │                      │
│                  │ - Side    │                      │
│                  │   effects │                      │
│                  └───────────┘                      │
└─────────────────────────────────────────────────────┘
```

**Models:**
- Agent reasoning: DeepSeek R1 or Qwen-QwQ (chain-of-thought reasoning models, China-hosted)
- Canvas generation: Fine-tuned LLM on canvas JSON schemas + 100+ example canvases. Output validated against node_type_registry schemas before execution.
- Cross-canvas optimization: Multi-objective optimization (maximize conversion, minimize fatigue, respect budget). Linear programming or reinforcement learning with canvas execution as environment.

**Key Technical Decisions:**
- Agent actions go through the SAME validation pipeline as human operator actions (publish validation, trigger check, etc.)
- Every AI Agent action is audited in canvas_audit_log with actor = "AI_AGENT"
- Operator can set `ai_agent_mode: SUPERVISED` (AI proposes, human approves) or `AUTONOMOUS` (AI acts, human reviews after the fact)
- AI Agent cannot modify canvases it did not create without explicit operator permission
- Rate limit: AI Agent can create at most N new canvases per day (configurable, default 3)

**Data Requirements:**
- 12+ months of production canvas execution data with attribution
- 10,000+ users with complete prediction profiles
- Proven A/B experiment framework with statistical significance engine
- Complete message receipt tracking (delivery, open, click, conversion)

### 3.3 User-Facing Features

| Feature | What the Marketer Sees |
|---------|----------------------|
| Goal-driven campaign | New "AI Strategy" page. Define goal: "Increase repeat purchase rate by 15% in Q4." Set constraints: budget 50K, channels [Push, SMS, WeChat], max frequency 3/week. Click "Launch Strategy." AI creates canvases, monitors, adapts. |
| NL canvas builder | Chat-like interface: type campaign description → AI generates canvas preview → operator edits/approves → canvas created. |
| Cross-canvas dashboard | New "Cross-Canvas View": shows all active canvases targeting the same users. AI highlights conflicts and proposes resolutions. "3 canvases targeting User X this week — AI recommends suppressing Canvas C (lowest predicted impact)." |
| Conversational debugging | Chat panel in canvas execution trace view: "Why didn't User 12345 get the Push?" AI responds: "User was filtered at IF_CONDITION node 'VIP Check' because their CLV band is 'low'. The ScoringHandler scored them at 35 (threshold: 50). Their purchase frequency is 0.3/month." |
| Strategy recommendations | Notification: "AI has detected an opportunity: users who abandoned cart in last 7 days show 3x higher responsiveness to SMS than Push. Recommend creating an SMS-first cart abandonment canvas. [Create Canvas] [Dismiss]" |
| Model performance dashboard | "AI Model Health" page: accuracy trends for all 4 prediction models, retrain history, A/B test results for model updates, feature importance changes. |

### 3.4 Dependencies

| Dependency | Status | Impact If Missing |
|-----------|--------|------------------|
| Plugin system for dynamic node registration | MISSING (new nodes need Java code + restart) | BLOCKS 3.2 — NL canvas builder can't create valid JSON for nodes it doesn't know about at runtime |
| Cross-canvas awareness layer | MISSING (each canvas is an island) | BLOCKS 3.3 — agent can't optimize what it can't see |
| Canvas audit log implementation | Table exists, code doesn't write to it | BLOCKS all — no accountability for AI actions |
| Attribution system with multi-touch models | MISSING | BLOCKS 3.1, 3.4, 3.5 — agent can't measure goal achievement |
| 12+ months production data | Not yet accumulated | BLOCKS 3.4 — model retraining needs longitudinal data |
| Real channel connectors (not WireMock) | ALL MOCK | BLOCKS all — agent managing mock channels is meaningless |
| A/B experiment statistical engine | MISSING | BLOCKS 3.4, 3.5 — can't evaluate model/content improvements |
| Operator notification system (email/webhook) | MISSING | BLOCKS 3.7 — AI recommendations can't reach operators |

### 3.5 Effort Estimate

| Capability | Effort (person-months) | Breakdown |
|-----------|----------------------|-----------|
| 3.1 Goal-Driven Campaign Orchestration | 4.0 | Agent core loop: 1.5, Canvas creation API: 1.0, Goal tracking: 0.5, Guardrails: 0.5, Testing: 0.5 |
| 3.2 Natural Language Canvas Builder | 3.0 | Schema-aware LLM: 1.5, JSON validation: 0.5, Frontend chat UI: 0.5, Testing: 0.5 |
| 3.3 Cross-Canvas Optimization Agent | 3.0 | Cross-canvas data layer: 1.0, Optimization algorithm: 1.0, Conflict resolution UI: 0.5, Testing: 0.5 |
| 3.4 Self-Improving Prediction Models | 2.0 | Auto-retrain pipeline: 1.0, Model A/B framework: 0.5, Monitoring: 0.5 |
| 3.5 AI Content A/B Optimization | 2.0 | Multi-variant generation: 0.5, Per-segment convergence: 1.0, Dashboard: 0.5 |
| 3.6 Conversational Canvas Debugging | 2.5 | Trace reasoning engine: 1.5, NL explanation: 0.5, Frontend chat: 0.5 |
| 3.7 Proactive Strategy Recommendations | 2.0 | Pattern detection: 0.5, Recommendation engine: 1.0, Notification integration: 0.5 |
| **Phase 3 Total** | **18.5** | ~9 months with 2 people, or ~6 months with 3 people |

### 3.6 Success Metrics

| Metric | Target (36-month) | How to Measure |
|--------|-----------------|----------------|
| AI-created canvases | 30%+ of new canvases created or co-created with AI | Canvas audit log (actor = AI_AGENT) |
| AI Agent decision accuracy | 80%+ of AI-proposed strategies achieve stated goal | Goal tracking system |
| Cross-canvas conflict auto-resolution | 50%+ of cross-canvas conflicts auto-resolved by AI | Conflict resolution audit log |
| NL canvas builder success rate | 70%+ of generated canvases pass validation on first attempt | Canvas creation logs |
| Conversational debugging satisfaction | 90%+ of debugging queries answered correctly | Operator feedback |
| Model auto-retrain frequency | Every 2 weeks without accuracy degradation | Model registry |
| Operator time savings | 40%+ reduction in time from campaign idea to launch | Time tracking |
| Revenue attributable to AI-driven campaigns | 20%+ of platform-attributed revenue comes from AI-created canvases | Attribution system |

---

## 4. Cumulative Investment Summary

| Phase | Timeline | Person-Months | Team Size | Calendar Duration | Cumulative PM |
|-------|----------|---------------|-----------|-------------------|---------------|
| Phase 1: AI-Assisted | 0-6 mo | 8.0 | 2-3 | 3-4 months | 8.0 |
| Phase 2: AI-Automated | 6-18 mo | 11.5 + 3.5 (attribution prereq) | 2-3 | 5-7 months | 23.0 |
| Phase 3: AI Agent | 18-36 mo | 18.5 | 3-4 | 5-9 months | 41.5 |
| **Total** | **0-36 mo** | **41.5** | | | |

**Critical Path of Prerequisites (not included in AI effort but required):**

| Prerequisite | Effort | When Needed | Consequence of Delay |
|-------------|--------|------------|---------------------|
| Attribution system | 3.5 PM | Before Phase 2 | AI scoring stays correlational; no ROI measurement; agent can't measure goal achievement |
| Message receipt tracking | 2.0 PM | Before Phase 2 | No channel engagement data; channel affinity model impossible |
| Experiment statistical engine | 2.0 PM | Before Phase 2.8 | No auto A/B promotion; no model improvement validation |
| Real channel connectors | 3.0 PM | Before Phase 2 | All engagement data is fake; all prediction models train on noise |
| Notification system (email/webhook) | 1.0 PM | Before Phase 2.7 | AI alerts can't reach operators; AI recommendations invisible |
| Cross-canvas awareness layer | 2.0 PM | Before Phase 3.3 | Agent operates blind; can't optimize globally |
| Canvas audit log implementation | 0.5 PM | Before Phase 3 | No accountability for AI actions |
| **Prerequisite Total** | **14.0 PM** | | |

**Total investment including prerequisites: 55.5 person-months over 36 months.**

---

## 5. Risk Assessment

### 5.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|-----------|
| LLM API cost explosion | HIGH | HIGH — per-canvas-node LLM calls at scale can exceed infrastructure cost | Token budget per canvas per day; cost alerts at 80% budget; fallback to cheaper models; cache frequent prompts |
| LLM latency in canvas execution path | MEDIUM | HIGH — canvas node timeout is 30s; complex prompts can exceed this | Pre-resolve templates at publish time; use smaller models for routing decisions; async AI_LLM with WAIT node |
| Prediction model drift | HIGH | MEDIUM — user behavior changes, model accuracy degrades | Phase 3 auto-retrain; weekly accuracy monitoring; automatic fallback to rule-based when AUC drops below threshold |
| Data privacy violations in LLM prompts | MEDIUM | CRITICAL — sending PII to external LLM APIs violates PIPL | PII scrubbing before prompt rendering; on-premise LLM option (Ollama provider); data residency configuration per provider |

### 5.2 Product Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|-----------|
| AI features unused by operators | MEDIUM | HIGH — AI features that nobody uses are expensive technical debt | Co-design with 3 pilot customers; measure adoption weekly; kill features with <5% adoption after 2 months |
| AI recommendations wrong/confusing | MEDIUM | HIGH — wrong recommendation erodes trust faster than no recommendation | Confidence scores on all AI outputs; show reasoning; default to "suggestion" mode not "auto" mode |
| Operator deskilling | LOW | MEDIUM — operators rely on AI and lose ability to manually create campaigns | Keep manual canvas creation as primary path; AI as optional enhancement |
| Competitive leapfrog | MEDIUM | MEDIUM — Customer.io or Klaviyo enters China with Agent capabilities | Focus on China-specific differentiators (WeChat, PIPL, DeepSeek integration) that foreign platforms can't easily replicate |

---

## 6. Phase Sequencing Visual

```
Month:    0   2   4   6   8   10  12  14  16  18  20  24  30  36
          │   │   │   │   │   │   │   │   │   │   │   │   │   │
Phase 1:  ├───┼───AI_LLM──┼───┤   │   │   │   │   │   │   │   │
          │   │  Churn Pred│Smart│  │   │   │   │   │   │   │   │
          │   │  Dashboard │Timing│  │   │   │   │   │   │   │   │
          │   │  Templates │Analytics│ │   │   │   │   │   │   │
          │   │   │   │   │   │   │   │   │   │   │   │   │   │
Prereqs:  │ Attribution ──┤   │   │   │   │   │   │   │   │   │
          │ Msg Receipt ──┤   │   │   │   │   │   │   │   │   │
          │ Real Channels─┤   │   │   │   │   │   │   │   │   │
          │   │   │   │   │   │   │   │   │   │   │   │   │   │
Phase 2:  │   │   │   │   ├───┼───┤   │   │   │   │   │   │   │
          │   │   │   │   │ CLV│Next│ChAff│   │   │   │   │   │
          │   │   │   │   │ SendOpt│Fatigue│Anomaly│A/B│  │   │
          │   │   │   │   │   │   │   │   │   │   │   │   │   │
Phase 3:  │   │   │   │   │   │   │   │   ├───┼───┼───┤   │   │
          │   │   │   │   │   │   │   │   │Goal│NL  │Cross│Self│
          │   │   │   │   │   │   │   │   │Driven│Builder│Canvas│Imp│
          │   │   │   │   │   │   │   │   │   │   │   │   │   │
```

---

## 7. Decision Framework: Build vs. Integrate vs. Defer

| Capability | Decision | Rationale |
|-----------|----------|-----------|
| AI_LLM node + multi-provider gateway | **BUILD** | Core platform capability; architecture already designed; integrates with existing handler pattern |
| Prompt template system | **BUILD** | Database-driven, extensible; differentiator through domain-specific templates |
| Churn / CLV / Next Purchase prediction | **BUILD** (model) + **INTEGRATE** (compute) | Build the model logic and canvas integration. Use Alibaba Cloud PAI or Volcengine ML Platform for compute — don't build ML infrastructure |
| Smart timing (best_send_hour) | **BUILD** (simple) | Aggregation query on event_log; no ML needed for Phase 1 |
| LLM API hosting | **INTEGRATE** | DeepSeek/Qwen APIs; never self-host LLMs |
| AI copywriting | **TEMPLATE** | Ship as AI_LLM template, not a separate feature. Zero additional engineering beyond the template |
| Image generation | **DEFER** | Not in scope; users can integrate via API_CALL |
| Conversational AI / chatbot | **DEFER** | Different product category; does not fit canvas execution model |
| NL canvas builder | **DEFER to Phase 3** | High risk; requires mature validation and schema system |
| AI Agent (Cowork-style) | **DEFER to Phase 3** | Requires plugin system, real channels, cross-canvas awareness — all currently missing |
