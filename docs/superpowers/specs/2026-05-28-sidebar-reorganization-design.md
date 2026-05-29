# Sidebar Menu Reorganization Design

## Problem

The "系统设置" menu group currently contains 12 sub-items, mixing business features (人群管理, AB实验) with platform configs (系统选项, 租户管理). This creates visual clutter and semantic confusion.

## Solution

Reorganize into 5 distinct top-level menu groups, following industry convention of separating business features from system configuration.

## New Structure

```
首页
自动化营销   → 旅程管理
用户洞察     → CDP 用户中心
数据管理     → 人群管理、标签配置、ID 类型配置、标签导入、AB 实验管理
集成配置     → API 接口配置、数据源配置、MQ 消息配置、事件配置
系统设置     → 系统选项配置、租户管理、用户管理
开发者文档   → API 说明
```

## Icons

| Menu Group | Icon | Ant Design Component |
|---|---|---|
| 数据管理 | DatabaseOutlined | data/storage imagery |
| 集成配置 | ApiOutlined | integration/connection imagery |
| 系统设置 | SettingOutlined | unchanged |

## Menu Key Mapping

| Menu Group Key | Sub-item Keys |
|---|---|
| `marketing` | `canvas` |
| `insight` | `cdp-users` |
| `data` | `audiences`, `tag-config`, `identity-types`, `tag-import`, `ab-experiments` |
| `integration` | `api-config`, `data-source-config`, `mq-config`, `event-config` |
| `settings` | `system-options`, `admin-tenants`, `admin-users` |
| `developer` | `api-docs` |

## Scope of Changes

Only `AppLayout.tsx` needs modification:

1. **Menu structure**: Split "系统设置" children into 3 groups (数据管理, 集成配置, 系统设置)
2. **Menu keys**: New top-level keys `data`, `integration`
3. **openKeys logic**: Map data-management keys to `['data']`, integration keys to `['integration']`
4. **Icon imports**: No new icons needed (DatabaseOutlined, ApiOutlined already imported)
5. **Visibility**: 数据管理 and 集成配置 remain admin-only (same permission as current 系统设置)

No changes to:
- Route paths (App.tsx)
- Page components
- API services
- Permission guards

## openKeys Mapping

```typescript
// 数据管理子项 → 展开 'data'
if (['audiences', 'tag-config', 'identity-types', 'tag-import', 'ab-experiments'].includes(selectedKey))
  return ['data']

// 集成配置子项 → 展开 'integration'
if (['api-config', 'data-source-config', 'mq-config', 'event-config'].includes(selectedKey))
  return ['integration']

// CDP → 展开 'insight'
if (selectedKey === 'cdp-users') return ['insight']

// 开发者文档
if (selectedKey === 'api-docs') return ['developer']

// 系统设置（剩余3项）
if (['system-options', 'admin-tenants', 'admin-users'].includes(selectedKey))
  return ['settings']
```