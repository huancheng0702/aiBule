---
name: airing-openapi
description: Integrates aiRing APP APIs against the project OpenAPI definition. Use when对接接口、写 common/api、联调后端、查找 /appApi 路径、对照接口文档，或用户提到 OpenAPI / 接口文档 / healthCenter / healthArchive。
---

# aiRing OpenAPI 对接

对接本项目后端接口时，**必须以本 skill 内的 OpenAPI 文档为准**，不要凭记忆编造路径或字段。

## 权威文档

完整接口定义见：[openapi-definition.md](openapi-definition.md)

文档来源：`OpenAPI definition.md`（APP `/appApi/*`）。若用户提供了更新版文档，先覆盖该文件再对接。

## 对接流程

1. **读页面/组件**：列出 UI 区块、本地 mock 数据、交互（查询 / 保存 / 翻页 / 弹窗）。
2. **查文档**：在 `openapi-definition.md` 中按模块名或路径关键词检索（如 `healthCenter`、`healthArchive`）。
3. **对照映射**：每个 UI 能力对应「方法 + 路径 + 入参 + 用途」；无匹配接口则标明「文档暂无 / 需产品确认」。
4. **封装 API**：在 `common/api/` 新增或扩展模块，沿用现有风格：
   - `uni.$get` / `uni.$post` / `uni.$put` / `uni.$delete`
   - 路径以 `/appApi/...` 开头
   - 导出命名函数，不在页面里写裸 URL
5. **页面联调**：替换 mock；处理空态、加载失败；`archiveType` 等枚举与文档保持一致。
6. **先清单后改码**：用户若只问「需要对接哪些接口」，先给出对照表，确认后再改代码。

## 项目约定

- 已有封装参考：`common/api/manualHealth.js`、`common/api/deviceData.js` 等
- 样式：App 端禁止 `calc()`（见 `.cursor/rules/css-no-calc.mdc`）
- 响应多为 `object`，字段以实际联调为准；文档缺字段时用真实返回补齐，并在注释中注明

## 常用模块索引

| 文档章节 | 路径前缀 | 典型页面 |
| --- | --- | --- |
| APP健康中心 | `/appApi/healthCenter/*` | 健康画像、健康中心首页 |
| 健康档案 | `/appApi/healthArchive/*` | 健康画像档案区 |
| APP健康异常记录 | `/appApi/healthAbnormal/*` | 异常记录列表/详情 |
| APP健康周报 | `/appApi/healthReport/*` | 周报/日报 |
| APP用户管理 | `/appApi/user/*` | 个人信息 |
| 用户设备 | `/appApi/device/*` | 设备绑定/状态 |

## 输出对照表示例

向用户说明待对接接口时，使用：

```markdown
| 页面能力 | 方法 | 路径 | 主要参数 | 说明 |
| --- | --- | --- | --- | --- |
| 综合分/解读 | GET | /appApi/healthCenter/index | date? | ... |
```
