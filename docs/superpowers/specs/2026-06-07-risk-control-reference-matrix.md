# Risk Control Rule Engine Reference Matrix

## 1. Purpose

This matrix records which external references are strong enough to influence the Canvas risk-control rule engine design, what each reference supports, and which parts must not be copied as if they were public implementation contracts.

It complements:

- `docs/superpowers/specs/2026-06-06-risk-control-rule-engine-design.md`
- `docs/superpowers/plans/2026-06-06-risk-control-rule-engine.md`

## 2. Evidence Tiers

| Tier | Meaning | How It Can Be Used |
| --- | --- | --- |
| Strong | Official documentation, official source repository, or first-party engineering article with stable enough content | Product capability, architecture pattern, operational control, or technology selection basis |
| Medium | Synchronized copy of first-party article, third-party developer community copy, or product page with limited implementation detail | Architecture and governance pattern, with source-access limitation recorded |
| Weak | News article, launch report, marketing copy, or search snippet only | Directional product inspiration only |
| Rejected | Unverified code, unverifiable API claim, unrelated tutorial, or source with unclear provenance | Not used for design decisions |

## 3. Source Matrix

| Reference | Tier | What It Supports | Canvas Design Landing |
| --- | --- | --- | --- |
| Antom Shield overview: `https://docs.antom.com/ac/antomshield_zh-cn/overview` | Strong | Product framing for payment risk services, configurable protection, analysis, and operational dashboard | Risk Control Platform split into Decision Runtime, Strategy Studio, and Risk Intelligence |
| Antom Shield rules: `https://docs.antom.com/ac/antomshield_zh-cn/rules` | Strong | Visual rule configuration and risk-control rule management | Strategy Studio rule editor, governed rule lifecycle, no-code rule authoring |
| Antom Shield risk level: `https://docs.antom.com/ac/antomshield_zh-cn/risklevel` | Strong | Risk score and risk level concepts | 0-100 score, low/medium/high bands, action mapping, model score normalization |
| Antom Shield lists: `https://docs.antom.com/ac/antomshield_zh-cn/list` | Strong | List management as a first-class risk-control capability | Tenant-scoped black, white, gray, observation lists with expiry, approval, masked values, and hit ledger |
| Antom Shield simulation: `https://docs.antom.com/ac/antomshield_zh-cn/simulation` | Strong | Pre-release simulation and impact assessment | Risk Lab simulation, replay reports, hit-rate and action-diff analysis before activation |
| Antom Shield analysis: `https://docs.antom.com/ac/antomshield_zh-cn/analysis` | Strong | Risk analysis and group/association investigation | First-stage explainable graph analysis and group-risk workbench |
| Antom Shield supported attributes: `https://docs.antom.com/ac/antomshield_zh-cn/supported-attributes` | Strong | Input attribute catalog for rule construction | Event schema registry, factor dictionary, validation against known attributes |
| Antom Shield data quality and security SDK: `https://docs.antom.com/ac/antomshield_zh-cn/getstarted` | Strong | Secure data collection and quality expectations | Source integrity checks, feature quality metrics, fail policy for missing or low-quality data |
| Old Alipay risk score product intro: `https://doc.open.alipay.com/doc2/detail?articleId=105271&docType=1&treeId=214` | Medium | External risk-score product shape | External score adapter and normalized score response |
| Old Alipay risk score quick start: `https://doc.open.alipay.com/docs/doc.htm?treeId=214&articleId=105182&docType=1` | Medium | API-style integration pattern for risk scoring | Model gateway and third-party risk score provider adapter |
| Old Alipay API list: `https://doc.open.alipay.com/docs/doc.htm?treeId=214&articleId=105270&docType=1` | Medium | Product API grouping | External integration contract grouping |
| `alipay.security.risk.rainscore.query`: `https://api.alidayu.com/docs/api.htm?docType=4&apiId=1048` | Medium | Score, info code, and label-style response model | Response fields inspire `score`, `riskBand`, `labels`, and `reasons`, but Canvas keeps richer audit trace |
| Meituan Zeus original: `https://tech.meituan.com/2020/05/14/meituan-security-zeus.html` | Medium | Historical first-party location for Zeus article | Recorded as historical origin only because it currently returns 404 |
| Meituan Zeus synchronized article: `https://cloud.tencent.com/developer/news/628286` | Strong for content, Medium for provenance | Common-node access, independent service API, scene/rule-group/rule/factor layering, extension functions, cumulative factors, decision-table factors, list factors, tool factors, mark, dual-run, replay, rule execution analysis, anti-misoperation controls | Core metadata boundaries, factor taxonomy, `MARK`/`DUAL_RUN`/`SIMULATION` lifecycle, replayable trace, peak-hour freeze, validation-result backfill |
| Meituan Zeus synchronized article: `https://segmentfault.com/a/1190000022653962` | Medium | Secondary access to the same Zeus article | Cross-check for Zeus capabilities when original is unavailable |
| Meituan Zeus ZGC original: `https://tech.meituan.com/2020/08/06/new-zgc-practice-in-meituan.html` | Medium | Historical first-party location for runtime article | Recorded as historical origin only because it currently returns 404 |
| Meituan Zeus ZGC synchronized article: `https://segmentfault.com/a/1190000023568163` | Strong for content, Medium for provenance | Aviator-backed rule execution at high rule volume, ClassLoader and CodeCache pressure, low-latency JVM operation | Structured DSL as hot path, Aviator only as governed expression sub-layer, bounded compile cache, CodeCache alerts, class-loading budget |
| Youzan risk-control rule engine: `https://tech.youzan.com/rules-engine/` | Strong | Drools-based realtime rule engine, realtime feature store, rule management center, offline tasks, operations platform, 100ms target | Separation of rule runtime, feature store, offline tasks, ops console, and model-as-complement pattern |
| Drools introduction: `https://docs.drools.org/latest/drools-docs/drools/introduction/index.html` | Strong | BRMS and rule-engine concepts | Drools is suitable for compatibility, experiments, and non-hot-path decision management but not first-slice hot path |
| Drools DMN docs: `https://docs.drools.org/latest/drools-docs/drools/DMN/index.html` | Strong | DMN decisions, FEEL, decision tables, business knowledge model | DMN-compatible decision table import and validation, normalized internal model |
| OpenL Tablets developer guide: `https://openldocs.readthedocs.io/en/latest/documentation/guides/developer_guide` | Strong | Spreadsheet/tablet-style decision logic for business users | Excel/CSV-style decision table import, conflict detection, coverage checks |
| Camunda DMN hit policy: `https://docs.camunda.io/docs/components/best-practices/modeling/choosing-the-dmn-hit-policy/` | Strong | Decision-table hit policy choices and consequences | Support `FIRST`, `UNIQUE`, `COLLECT`, and `PRIORITY` in internal decision tables with explicit semantics |
| AviatorScript README: `https://github.com/killme2008/aviatorscript/blob/master/README-EN.md` | Strong | JVM-hosted scripting language that compiles scripts to bytecode and evaluates on the fly | Safe-expression sub-layer, compile cache, timeout, function whitelist, no arbitrary reflection/file/network/class loading |
| Apache Flink async I/O: `https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/operators/asyncio/` | Strong | Async calls to external stores in streaming pipelines | Realtime feature jobs can enrich events with async external feature reads while protecting operator latency |
| Apache Flink state docs: `https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/state/` | Strong | Stateful stream processing and state TTL | Rolling-window counters, distinct counts, and bounded state for realtime aggregate factors |
| OpenTelemetry Java docs: `https://opentelemetry.io/docs/languages/java/` | Strong | Java traces, metrics, and logs instrumentation | Decision trace correlation, rule-hit metrics, feature-missing counters, latency histograms, model gateway spans |
| OpenTelemetry overview: `https://opentelemetry.io/docs/` | Strong | Vendor-neutral telemetry framework | OTel as common observability contract feeding Prometheus/Grafana and trace backends |
| AIR Engine launch report: `https://www.cfbond.com/2024/06/25/wap_991052868.html` | Weak | Product direction: AI-assisted risk engine | AI Copilot direction only; no API or implementation dependency |
| AIR Engine launch report: `https://www.bianews.com/news/details?id=188342` | Weak | Product direction: AI-assisted risk engine | AI-generated rule drafts and expert-assisted operations as long-term product capabilities |

## 4. Design Decisions Proven By Multiple Sources

### 4.1 Product Shape

Antom Shield, Zeus, and Youzan all support treating risk control as a platform rather than embedding hardcoded checks in business services. The Canvas design therefore uses a standalone Risk Control Platform with:

- Online Decision Runtime.
- Strategy Studio.
- Risk Lab.
- Risk Intelligence.
- API, MQ, batch, and Canvas-node access.

### 4.2 Rule Model

Zeus gives the strongest public support for the scene, rule group, rule, and factor structure. Antom Shield supports visual rule management, and DMN/OpenL supports tabular decisions. The Canvas design therefore separates:

- Scenes.
- Strategy versions.
- Rule groups.
- Rules.
- Factors.
- Lists.
- Decision tables.
- Actions.
- Trace records.

### 4.3 Runtime Choice

Zeus ZGC experience and AviatorScript documentation show that expression engines are useful but require compile and class-loading governance. Drools/DMN/OpenL show mature decision-management concepts, but they are heavier than needed for the first hot path. The Canvas design therefore chooses:

- Structured DSL and Java AST evaluator for the online hot path.
- AviatorScript only for governed expressions.
- DMN/OpenL/Excel imported into an internal decision-table model.
- Drools compatibility as optional non-hot-path capability.

### 4.4 Release Governance

Zeus and Antom Shield both support validating strategies before full enforcement. The Canvas design therefore requires:

- Simulation.
- Mark mode.
- Shadow mode.
- Dual-run mode.
- Canary rollout.
- Approval.
- Emergency pause.
- Rollback.

### 4.5 Feature Platform

Zeus cumulative factors, Youzan realtime feature store, and Flink state/async I/O docs support a dedicated feature platform. The Canvas design therefore separates:

- Raw event fields.
- Realtime aggregate features.
- Offline features.
- List lookups.
- Model scores.
- Graph features.
- Feature quality and TTL controls.

### 4.6 Observability

Zeus rule analysis, Antom Shield analysis, and OpenTelemetry docs support making observability part of the product instead of a late add-on. The Canvas design therefore requires:

- Decision trace.
- Rule execution replay.
- Rule-hit analytics.
- Feature-missing metrics.
- Action distribution.
- Latency histograms.
- Model and feature dependency spans.

## 5. Insufficient Public Evidence

The public corpus does not provide:

- Antom Shield internal execution architecture.
- AIR Engine API, SDK, or internal implementation details.
- Zeus source code, API schema, database schema, or exact control-plane implementation.
- Youzan production schema, exact Drools rule templates, or feature-store internals.
- A public vendor-agnostic schema for risk-control decision traces.

Canvas must therefore define its own contracts for:

- Metadata tables.
- Online decision API.
- Trace schema.
- Rule DSL AST.
- Factor registry.
- Strategy version lifecycle.
- Audit ledger.
- Rollback semantics.

Those Canvas-owned contracts are defined in `docs/superpowers/specs/2026-06-07-risk-control-contracts.md`.

Requirement-to-implementation acceptance tracking is defined in `docs/superpowers/specs/2026-06-07-risk-control-traceability-matrix.md`.

## 6. Product Sufficiency Assessment

The available documents are sufficient to design a production-grade product at architecture, data model, API, governance, and operations level. They are not sufficient to clone any vendor product or rely on a vendor-compatible internal behavior.

The resulting product should be evaluated against Canvas needs, not similarity to any one source:

- Low-latency decision serving.
- Tenant isolation.
- Governed authoring.
- Safe rollout.
- Replayable trace.
- Feature quality.
- Model governance.
- Operational runbook.
- Audit and compliance.
