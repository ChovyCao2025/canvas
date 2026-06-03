# 方向㊶：数据隐私治理与合规自动化 — 功能清单

> 定位：从"Consent同意管理"升级为"完整隐私治理平台"——DSAR自动化+数据映射+保留策略+隐私影响评估(PIA)+Cookie合规+供应商隐私评估
> 策略评估：2026年全球隐私法规持续收紧(GDPR/CCPA/PIPL/APA/CPA/CTDPA等15+法规)，DataGrail报告DSAR请求量年增40%+，OneTrust/Ketch估值$10B+，合规是SaaS平台的生存底线
> 竞品对标：OneTrust(企业隐私管理)、Ketch(全隐私编排)、DataGrail(DSAR自动化)、BigID(数据发现+分类)、Usercentrics(同意管理+CMP)
> 建议：**P1建议做**，依赖㉗偏好与同意管理中心+⑫多租户SaaS化成熟后启动，㉗解决"同意采集"，本方向解决"隐私治理运营"

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| DataGrail: 2026 Guide to DSAR Automation | 2026州级隐私法激增+无治愈期+DSAR请求量增40%→DSAR自动化是刚需 | https://www.datagrail.io/blog/data-privacy/the-2026-guide-to-dsar-automation/ |
| Ketch: Best Data Privacy Software 2026 | OneTrust/Ketch/BigID: 企业隐私平台+DSAR+数据映射+同意管理 | https://www.ketch.com/blog/posts/best-data-privacy-software |
| Usercentrics: 8 Best Data Privacy Management Software 2026 | 8款隐私管理软件对比：敏感数据管理+用户同意+DSAR | https://usercentrics.com/knowledge-hub/data-privacy-management-tools/ |
| TrustArc: Privacy Management Platform 2026 | 隐私管理平台核心：DSAR+数据映射+评估+供应商风险 | https://trustarc.com/resource/how-to-evaluate-privacy-management-platform/ |
| Reddit Fintech: Privacy Management Platforms 2026 | GDPR/CCPA合规→DSAR+数据映射+同意管理的综合需求 | https://www.reddit.com/r/fintech/comments/1rekyvh/ |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 同意管理 | **部分** | MarketingConsentDO: OPT_IN/OPT_OUT | 仅基础同意状态，无完整CMP |
| 偏好中心 | **规划中** | ㉗方向规划 | 偏好中心尚未实现 |
| DSAR处理 | **不存在** | — | 无数据主体访问请求(DSAR)受理/跟踪/响应流程 |
| 数据映射 | **不存在** | — | 无数据流图(哪些系统存储哪些个人数据) |
| 保留策略 | **不存在** | — | 无数据生命周期管理(自动归档/删除过期数据) |
| Cookie管理 | **不存在** | — | 无Cookie扫描+分类+同意横幅管理 |
| PIA/DPIA | **不存在** | — | 无隐私影响评估模板+流程 |
| 供应商评估 | **不存在** | — | 无第三方供应商隐私风险评估 |

---

## 功能清单

### P0 — DSAR自动化与数据映射

#### 1. DSAR自动化处理 [中复杂度 | 1.5人月]

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 请求入口 | 用户自助提交DSAR(访问/删除/更正/导出/限制处理) | DataGrail: self-service DSAR portal |
| 身份验证 | 提交DSAR前验证身份(双因素/证件上传) | Ketch: identity verification |
| 自动数据发现 | 请求受理后自动扫描所有系统→汇总用户个人数据 | DataGrail: automated data discovery |
| 响应生成 | 自动生成DSAR响应包(数据副本+处理说明) | DataGrail: automated response |
| SLA追踪 | 法规SLA自动计时(GDPR 30天/CCPA 45天)+逾期预警 | DataGrail: deadline tracking |
| 审计报告 | DSAR全流程审计日志(谁/什么时候/做了什么/结果) | TrustArc: audit trail |

**DSAR DDL**：

```sql
CREATE TABLE dsar_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64),
    request_type VARCHAR(20) NOT NULL COMMENT 'ACCESS/DELETION/CORRECTION/PORTABILITY/RESTRICTION/OBJECTION',
    request_source VARCHAR(20) COMMENT 'PORTAL/EMAIL/PHONE/MAIL/IN_PERSON',
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/VERIFYING/IN_PROGRESS/READY_FOR_REVIEW/COMPLETED/REJECTED/EXPIRED',
    regulatory_deadline DATE NOT NULL COMMENT '法定截止日期',
    data_sources JSON COMMENT '扫描发现的数据所在系统',
    response_data JSON COMMENT '汇总的个人数据',
    reviewer_notes TEXT,
    completed_at DATETIME,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_deadline (regulatory_deadline),
    INDEX idx_tenant (tenant_id)
) COMMENT 'DSAR请求';
```

#### 2. 数据映射与发现 [中复杂度 | 1.0人月]

| 子功能 | 描述 |
|--------|------|
| 数据流图 | 可视化展示所有系统间的个人数据流(收集→存储→处理→分享→删除) |
| 数据分类 | 自动/手动标记数据类别(姓名/邮箱/电话/身份证/C2/C3敏感) |
| 处理目的标注 | 每个数据字段标注处理目的+法律依据(同意/合同/法定义务/合法权益) |
| 数据清单 | 完整数据资产清单(哪个表/哪个字段/什么类型/什么目的/多久保留) |

### P1 — 保留策略与隐私评估

#### 3. 数据保留与生命周期管理 [中复杂度 | 1.0人月]

| 子功能 | 描述 |
|--------|------|
| 保留策略配置 | 按数据类型配置保留期限(营销数据2年/同意记录5年/订单数据7年) |
| 自动清理 | 到期数据自动归档→软删除→硬删除 |
| 删除验证 | 删除操作完成后验证数据确实被清除+生成删除证明 |
| 用户数据可移植 | 用户请求导出数据→标准格式(JSON/CSV)→下载链接 |

#### 4. 隐私影响评估(PIA/DPIA) [低复杂度 | 0.5人月]

| 子功能 | 描述 |
|--------|------|
| 评估模板 | 预置PIA/DPIA模板(符合GDPR/CCPA/PIPL要求) |
| 自动触发 | 新功能上线→涉及新数据处理→自动触发DPIA流程 |
| 审批工作流 | 评估→安全审核→法务审核→DPO批准 |
| 风险评分 | 自动风险评估(数据敏感度×处理规模×跨境传输→风险等级) |

### P2 — Cookie合规与供应商管理

#### 5. Cookie Consent管理 [低复杂度 | 0.5人月]
Cookie扫描+分类(必要/功能/分析/营销)+同意横幅+偏好面板+每次同意记录

#### 6. 供应商隐私评估 [低复杂度 | 0.5人月]
第三方供应商隐私评估(TPSA)+DPA管理+供应商风险评分+定期复审

---

## 工作量估算

| 优先级 | 功能 | 总计 |
|--------|------|------|
| P0 | DSAR自动化处理 | 1.7人月 |
| P0 | 数据映射与发现 | 1.1人月 |
| P1 | 数据保留与生命周期 | 1.1人月 |
| P1 | 隐私影响评估 | 0.6人月 |
| P2 | Cookie合规+供应商评估 | 1.2人月 |
| | **合计** | **5.7人月** |

## 关键依赖

㉗偏好与同意管理(同意是隐私治理前置) + ⑫多租户(不同租户不同合规框架) + ⑦合规渠道(隐私合规是合规底座一部分) + ⑯协作权限(DPIA审批流程)
