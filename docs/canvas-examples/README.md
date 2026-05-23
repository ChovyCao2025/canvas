# 画布示例库

画布示例库包含两类内容：

1. 官方模板画布：存储在 `canvas_template`。应用启动后，当 `canvas.examples.enabled=true` 时，`CanvasExampleSeeder` 会把启用的官方模板导入为普通草稿画布和 `canvas_version` 草稿版本。
2. 使用说明文档：说明每个组件怎么配置、怎么连线，以及在不同行业营销场景下怎么组合。

默认配置：

```yaml
canvas:
  examples:
    enabled: true
```

关闭后，应用不会补齐官方示例，画布列表会隐藏 `is_example=1` 的示例画布。

## 数据流

`canvas_template.template_key` 是官方模板的稳定唯一键。启动导入时通过 `canvas.source_template_key` 做幂等判断，避免重复生成示例画布。

用户从示例画布克隆，或从模板创建新画布时，新画布会明确写入 `is_example=0`、`source_template_key=null`，后续不再受示例开关影响。

## 文档入口

- [组件手册](components.md)
- [组合套路](combinations.md)
- [行业场景](scenarios.md)
