# OpenAPI definition

## APP用户管理
APP用户管理接口

# 编辑用户信息

**POST** `/appApi/user/editInfo`

可编辑昵称、性别、出生日期、身高、体重，传入哪个编辑哪个，传null清空对应数据

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | integer/int64 | 是 | 用户ID |
| `nickName` | string | 否 | 昵称（传null清空） |
| `avatar` | string | 否 | 头像URL（传null清空） |
| `gender` | string | 否 | 性别（1-男 2-女 0-未知，传null清空） |
| `birthday` | string/date | 否 | 出生日期（传null清空） |
| `height` | integer/int32 | 否 | 身高cm（传null清空） |
| `weight` | number | 否 | 体重kg（传null清空） |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 注销账号

**POST** `/appApi/user/cancel`

用户主动注销账号，清除敏感信息并退出登录

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 获取用户信息

**GET** `/appApi/user/info`

根据token获取当前登录用户详细信息

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP健康周报
用户健康周报

# 获取周报详情

**GET** `/appApi/healthReport/weekly`

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `date` | query | string | 否 | 结束日期 yyyy-MM-dd |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 获取周报列表

**GET** `/appApi/healthReport/weeklyList`

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `pageNum` | query | integer/int32 | 否 | 页码，默认1 |
| `pageSize` | query | integer/int32 | 否 | 每页数量，默认10 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 获取日报详情

**GET** `/appApi/healthReport/daily`

结构与周报一致，环比昨日，实时生成

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `date` | query | string | 否 | 日期 yyyy-MM-dd |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP内容/协议/政策
H5页面获取内容

# 获取内容详情

**GET** `/appApi/content/{contentId}`

根据ID获取富文本内容，供H5页面展示

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `contentId` | path | integer/int64 | 是 | 内容ID：1-用户协议 2-隐私政策 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP健康中心
健康中心首页：健康画像综合分、当日异常记录汇总

# 健康解读历史对比

**GET** `/appApi/healthCenter/portraitList`

从今天起往前推N天（含今天，默认3天）的每日健康画像，按日期倒序

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `days` | query | integer/int32 | 否 | 天数，默认3，范围1-30 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 异常指标记录历史对比

**GET** `/appApi/healthCenter/organArchiveCount`

往前推N天（含今天，默认3天）逐日统计档案快照中命中器官分类选项的项数（快照一行算一个），返回[date,count]列表，按日期升序

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `days` | query | integer/int32 | 否 | 天数，默认3，范围1-30 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 健康中心首页

**GET** `/appApi/healthCenter/index`

健康画像综合分（六维度加权）+ 当日异常记录汇总

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `date` | query | string | 否 | 基准日期 yyyy-MM-dd，默认今天 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP AI健康助手
AI健康助手对话、会话管理

# 新建会话

**POST** `/appApi/ai/session`

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 体检报告指标识别-视觉模型(非流式)

**POST** `/appApi/ai/report/recognize`

仅识别指标不分析，一次返回 [{key,value}] 列表。支持 file 上传或 imageUrl

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `imageUrl` | query | string | 否 |  |

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | string/binary | 否 |  |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 体检报告指标识别(只识别指标)-视觉模型(SSE流式)

**POST** `/appApi/ai/report/recognize/stream`

仅识别指标不分析，逐条推送 item 事件（data为单键值对{指标名:检测值}）。支持 file 上传或 imageUrl

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `imageUrl` | query | string | 否 |  |

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | string/binary | 否 |  |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | SseEmitter |

---

# 体检报告解析记录导出

**POST** `/appApi/ai/report/export`

导出当前用户解析记录为Excel；reportIds 逗号分隔，不传导出全部

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `reportIds` | query | integer/int64[] | 否 |  |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK |  |

---

# 体检报告AI分析(非流式)（识别指标加建议）

**POST** `/appApi/ai/report/analyze`

上传体检报告图片，结构化识别出key-value指标后结合戒指健康数据由AI分析异常指标并给出建议

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | string/binary | 是 |  |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 体检报告分析(非流式)

**POST** `/appApi/ai/report/analyzeText`

传入识别出的指标文本 reportText，一次返回完整分析建议

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `reportText` | query | string | 是 |  |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 体检报告分析(分析上一步的指标信息给出建议)(SSE流式)

**POST** `/appApi/ai/report/analyzeText/stream`

传入识别出的指标文本 reportText，流式推送分析建议(delta)

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `reportText` | query | string | 是 |  |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | SseEmitter |

---

# 体检报告AI分析(URL)

**POST** `/appApi/ai/report/analyzeByUrl`

传入体检报告图片URL，结构化识别key-value指标后结合戒指健康数据由AI分析

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `imageUrl` | query | string | 是 |  |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 体检报告AI分析-视觉模型(非流式)

**POST** `/appApi/ai/report/analyzeByAi`

与流式版同能力，一次返回完整 ReportAnalyzeVO（items+analysis）。App/小程序端推荐

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `imageUrl` | query | string | 否 |  |

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | string/binary | 否 |  |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 体检报告AI分析（指标+建议）-视觉模型(SSE流式)

**POST** `/appApi/ai/report/analyzeByAi/stream`

千问视觉模型直接读图，指标逐条流式推送(item)再流式推分析建议(delta)。支持 file 上传或 imageUrl 直传

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `imageUrl` | query | string | 否 |  |

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | string/binary | 否 |  |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | SseEmitter |

---

# AI对话(普通)

**POST** `/appApi/ai/chat`

发送消息，一次性返回完整回复。sessionId为空自动创建会话

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sessionId` | integer/int64 | 否 | 会话ID，为空则自动创建新会话 |
| `content` | string | 是 | 消息内容 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# AI对话(SSE流式)

**POST** `/appApi/ai/chat/stream`

发送消息，SSE流式返回AI回复。sessionId为空自动创建会话

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sessionId` | integer/int64 | 否 | 会话ID，为空则自动创建新会话 |
| `content` | string | 是 | 消息内容 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | SseEmitter |

---

# 会话消息列表

**GET** `/appApi/ai/session/{sessionId}/messages`

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `sessionId` | path | integer/int64 | 是 |  |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 会话列表

**GET** `/appApi/ai/session/list`

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 体检报告解析历史

**GET** `/appApi/ai/report/history`

返回当前用户所有解析记录，含指标明细(reportText/itemsJson)和分析建议(analysis)

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 体检报告解析记录详情

**GET** `/appApi/ai/report/history/{reportId}`

按 reportId 查询单条解析记录（仅本人）

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `reportId` | path | integer/int64 | 是 |  |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 快捷问题

**GET** `/appApi/ai/quickQuestions`

获取AI助手首页快捷问题列表（后台可配置）

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 删除会话

**DELETE** `/appApi/ai/session/{sessionId}`

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `sessionId` | path | integer/int64 | 是 |  |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## 设备配置管理
设备配置的保存与查询

# 保存每日目标

**POST** `/appApi/device/config/save`

保存或更新当前用户的每日目标（target配置，按用户唯一

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `step` | integer/int32 | 否 | 步数目标(步)，不传则不修改该项 |
| `calorie` | integer/int32 | 否 | 卡路里目标(千卡)，不传则不修改该项 |
| `sprotTime` | integer/int32 | 否 | 运动时长目标(分钟)，不传则不修改该项 |
| `deviceMac` | string | 否 | 设备MAC地址 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 查询设备配置

**GET** `/appApi/device/config/{configType}`

按配置类型查询当前用户的设备配置

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `configType` | path | string | 是 |  |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 查询所有设备配置

**GET** `/appApi/device/config/list`

查询当前用户的所有设备配置列表

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP认证管理
微信登录、短信验证码、绑定/换绑手机号

# 微信登录

**POST** `/appApi/auth/wxLogin`

通过微信code登录，返回token、用户ID、昵称、头像、是否绑定手机号

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `code` | string | 是 | 微信code（wx.login获取） |
| `nickName` | string | 否 | 昵称（微信getUserInfo获取，可选） |
| `avatar` | string | 否 | 头像URL（微信getUserInfo获取，可选） |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 发送短信验证码

**POST** `/appApi/auth/sendSmsCode`

向指定手机号发送验证码，60秒内不可重复发送

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `phone` | string | 是 | 手机号 |
| `scene` | string | 是 | 场景（bind-绑定手机号） |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 手机号验证码登录

**POST** `/appApi/auth/phoneLogin`

通过手机号和短信验证码登录，新用户自动注册

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `phone` | string | 是 | 手机号 |
| `code` | string | 是 | 验证码 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 一键登录

**POST** `/appApi/auth/oneKeyLogin`

通过创蓝闪验SDK返回的token置换手机号，自动登录/注册

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `token` | string | 是 | SDK返回的运营商token（一次性有效） |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 换绑手机号

**POST** `/appApi/auth/changePhone`

验证新手机号的短信验证码并换绑，新手机号需唯一且与当前不同

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | integer/int64 | 是 | 用户ID |
| `phone` | string | 是 | 新手机号 |
| `code` | string | 是 | 短信验证码 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 绑定手机号

**POST** `/appApi/auth/bindPhone`

验证短信验证码并绑定手机号，手机号需唯一

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | integer/int64 | 是 | 用户ID |
| `phone` | string | 是 | 手机号 |
| `code` | string | 是 | 短信验证码 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP设备数据
设备健康数据上报与查询

# 批量上报设备数据

**POST** `/appApi/deviceData/upload`

APP从蓝牙设备接收心率/血氧/血压/体温/HRV/心电/睡眠/运动数据后批量上报

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `deviceMac` | string | 否 | 设备MAC地址 |
| `deviceVersion` | string | 否 | 设备固件版本 |
| `batteryLevel` | integer/int32 | 否 | 设备电量百分比(0-100) |
| `dataList` | DeviceDataItemDTO[] | 是 | 数据列表 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 压力趋势

**GET** `/appApi/deviceData/trend/stress`

返回压力趋势图、统计值、状态

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `range` | query | string | 否 | 时间范围：day/week/month，默认day |
| `date` | query | string | 否 | 基准日期(yyyy-MM-dd)，默认今天 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 步数趋势

**GET** `/appApi/deviceData/trend/sport`

返回所选日期步数汇总（总步数/总距离/总卡路里）、每日目标设置及目标进度、最新记录、状态文案

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `date` | query | string | 否 | 查询日期(yyyy-MM-dd)，默认今天 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 睡眠趋势

**GET** `/appApi/deviceData/trend/sleep`

返回睡眠趋势。日视图：series为阶段点(value=阶段1入睡/2深睡/3浅睡/4苏醒/5快速眼动，value2=持续分钟数)，另含score得分/scoreText质量文本/scoreSegments得分标尺分段/segments睡眠结构分段；周/月视图按天返回总睡眠时长

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `range` | query | string | 否 | 时间范围：day/week/month，默认day |
| `date` | query | string | 否 | 基准日期(yyyy-MM-dd)，默认今天；睡眠归属入睡当晚(当日20:00~次日20:00) |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# HRV趋势

**GET** `/appApi/deviceData/trend/hrv`

返回心率变异性趋势图、统计值、状态

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `range` | query | string | 否 | 时间范围：day/week/month，默认day |
| `date` | query | string | 否 | 基准日期(yyyy-MM-dd)，默认今天 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 心率趋势

**GET** `/appApi/deviceData/trend/heartRate`

返回心率趋势图、统计值、状态。range=day（当天24小时）/week（本周）/month（本月）

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `range` | query | string | 否 | 时间范围：day/week/month，默认day |
| `date` | query | string | 否 | 基准日期(yyyy-MM-dd)，默认今天 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 血氧趋势

**GET** `/appApi/deviceData/trend/bloodOxygen`

返回血氧趋势图、统计值、状态

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `range` | query | string | 否 | 时间范围：day/week/month，默认day |
| `date` | query | string | 否 | 基准日期(yyyy-MM-dd)，默认今天 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 单次测量结果分析

**GET** `/appApi/deviceData/measureResult`

测量结束后按measureId返回结果值/状态/分段区间/参考范围/AI检测结果与趋势建议，支持heart_rate心率/blood_oxygen血氧/hrv心率变异性/stress压力

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `measureId` | query | string | 是 | 测量会话ID(App下发测量时生成) |
| `dataType` | query | string | 否 | 数据类型：heart_rate心率/blood_oxygen血氧/hrv心率变异性/stress压力，不传默认取该会话第一条数据的类型 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 查询设备数据列表

**GET** `/appApi/deviceData/list`

查询设备数据列表

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `dataType` | query | string | 否 | 数据类型：heart_rate心率/hrv心率变异性/blood_oxygen血氧/blood_pressure血压/body_temp体温/sport步数/sleep睡眠/mix_sport混合运动/ecg心电 |
| `startDate` | query | string | 否 | 开始日期(yyyy-MM-dd) |
| `endDate` | query | string | 否 | 结束日期(yyyy-MM-dd) |
| `measureId` | query | string | 否 | 测量会话ID(查询某一次App下发测量的数据) |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 查询最新设备数据

**GET** `/appApi/deviceData/latest`

按数据类型查询最新一条记录

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `dataType` | query | string | 是 | 数据类型：heart_rate心率/hrv心率变异性/blood_oxygen血氧/blood_pressure血压/body_temp体温/sport步数/sleep睡眠/mix_sport混合运动/ecg心电 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 设备上报数据看板

**GET** `/appApi/deviceData/dashboard`

按指定日期返回6个卡片的数值、进度/分段/曲线。steps项含步数/距离/卡路里/进度(progress)及目标值：target步数目标(默认10000)/distanceTarget距离目标(米)/calorieTarget卡路里目标(千卡)/sportTimeTarget活动时长目标(分钟)，均取自设备配置target

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `date` | query | string | 否 | 查询日期(yyyy-MM-dd)，不传默认今天 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 查询设备数据日聚合统计

**GET** `/appApi/deviceData/daily`

按天聚合统计min/max/avg/count

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `dataType` | query | string | 是 | 数据类型：heart_rate心率/hrv心率变异性/blood_oxygen血氧/blood_pressure血压/body_temp体温/sport步数 |
| `startDate` | query | string | 否 | 开始日期(yyyy-MM-dd) |
| `endDate` | query | string | 否 | 结束日期(yyyy-MM-dd) |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP关于我们
获取关于我们信息

# 获取关于我们信息

**GET** `/appApi/aboutUs`

返回关于我们的LOGO、名称、简介、联系人、联系方式

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP意见反馈
用户提交意见反馈

# 提交意见反馈

**POST** `/appApi/feedback/submit`

用户填写反馈内容和图片进行反馈

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `content` | string | 是 | 反馈内容 |
| `images` | string | 否 | 图片URL(多个逗号分隔) |
| `contact` | string | 否 | 联系方式 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP配置信息
获取APP相关配置信息

# 获取健康管家信息

**GET** `/appApi/configInfo/healthManager`

返回健康管家二维码、联系电话、服务时间，数据来源于APP配置。

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP常见问题
查询常见问题列表

# 查询常见问题列表

**GET** `/appApi/faq/list`

返回所有正常状态的常见问题，按排序字段排序

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `category` | query | string | 否 | 问题分类 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP健康异常记录
健康异常记录查询

# 重建异常记录

**POST** `/appApi/healthAbnormal/rebuild`

按设备原始数据重放合并历史异常记录，一次性数据修复用

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 异常记录详情

**GET** `/appApi/healthAbnormal/{abnormalId}`

按ID查询单条异常记录完整信息

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `abnormalId` | path | integer/int64 | 是 | 异常记录ID |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 查询异常记录列表

**GET** `/appApi/healthAbnormal/list`

支持按类型、时间段筛选，按测量时间倒序分页

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `dataType` | query | string | 否 | 数据类型，为空查全部 |
| `startTime` | query | string/date-time | 否 | 测量时间起 yyyy-MM-dd HH:mm:ss |
| `endTime` | query | string/date-time | 否 | 测量时间止 yyyy-MM-dd HH:mm:ss |
| `pageNum` | query | integer/int32 | 否 | 页码，默认1 |
| `pageSize` | query | integer/int32 | 否 | 每页条数，默认10 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP数据类型说明
查询手动记录数据类型的说明内容

# 查询数据类型说明列表

**GET** `/appApi/healthTypeInfo/list`

返回指定数据类型所有正常状态的说明区块，按排序字段排序。dataType：1-体重 2-血糖 3-体温 4-血压 5-经期

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `dataType` | query | string | 是 | 数据类型：1-体重 2-血糖 3-体温 4-血压 5-经期 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP经期管理
经期记录、日历预测、今日状态

# 切换今日经期状态

**POST** `/appApi/period/today`

标记或取消今天为经期日

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `isPeriod` | boolean | 否 | 是否处于经期 |
| `date` | string/date | 否 | 日期，默认今天 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 初始化经期

**POST** `/appApi/period/init`

保存最近经期开始日、持续天数、平均周期

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `lastStartDate` | string/date | 是 | 经期开始日期 |
| `duration` | integer/int32 | 是 | 经期持续天数 |
| `cycleLength` | integer/int32 | 是 | 平均周期天数 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 获取经期统计

**GET** `/appApi/period/statistics`

获取经期统计及下次预测

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 获取经期日历

**GET** `/appApi/period/calendar`

获取指定年月的经期日历及预测

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `yearMonth` | query | string | 否 | 年月，格式 yyyy-MM |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP轮播图
获取APP轮播图列表

# 获取轮播图列表

**GET** `/appApi/bannerInfo/list`

返回启用状态的轮播图列表，条数由配置app.banner.displayCount控制。

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP文件管理
文件上传、OSS直传签名

# 服务端上传文件

**POST** `/appApi/file/upload`

前端上传文件到后端，后端转发到OSS或本地存储，返回文件访问URL

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | string/binary | 是 |  |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 获取OSS直传签名

**GET** `/appApi/file/policy`

返回Policy签名信息，前端直接POST到OSS，上传完成后通过返回的domain+key拼接访问URL

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `fileName` | query | string | 否 |  |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP紧急联系人
紧急联系人增删改查

# 编辑紧急联系人

**PUT** `/appApi/emergency/edit`

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `contactId` | integer/int64 | 是 | 联系人ID |
| `contactName` | string | 是 | 联系人姓名 |
| `relation` | string | 是 | 关系 |
| `phone` | string | 是 | 联系人手机号 |
| `remark` | string | 否 | 备注 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 新增紧急联系人

**POST** `/appApi/emergency/add`

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `contactName` | string | 是 | 联系人姓名 |
| `relation` | string | 是 | 关系 |
| `phone` | string | 是 | 联系人手机号 |
| `remark` | string | 否 | 备注 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 获取关系列表

**GET** `/appApi/emergency/relations`

从配置中获取可选的关系列表

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 查询紧急联系人列表

**GET** `/appApi/emergency/list`

根据token获取当前用户的紧急联系人列表

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 删除紧急联系人

**DELETE** `/appApi/emergency/{contactId}`

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `contactId` | query | integer/int64 | 是 |  |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## APP端手动健康记录
APP端手动健康记录相关接口

# 查询当前用户首页手动健康记录数据显示配置

**GET** `/appApi/manualHealthRecord/display`

返回当前用户已配置的健康指标及首页显示开关，首次访问自动初始化默认全部显示。

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 更新首页健康数据显示状态

**PUT** `/appApi/manualHealthRecord/display`

切换某健康指标在首页的显示/隐藏，隐藏后仍可在健康档案中继续记录。

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `type` | string | 是 | 类型（1-体重 2-血糖 3-体温 4-血压 5-经期） |
| `isShow` | string | 是 | 是否显示在首页（0-否 1-是） |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 新增手动健康数据记录

**POST** `/appApi/manualHealthRecord/add`

根据类型自动设置单位，并对记录值做格式和范围校验。体重(1): 数字20~300 kg；血糖(2): 数字1~50 mmol/L；体温(3): 数字30~45 °C；血压(4): 收缩压/舒张压(60~260/40~200) mmHg。不做每日条数限制，每次提交均新增一条记录。

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `type` | string | 是 | 类型（1-体重 2-血糖 3-体温 4-血压） |
| `value` | string | 是 | 记录值。格式要求：体重-数字(20~300)，如72.5；血糖-数字(1~50)，如5.6；体温-数字(30~45)，如36.5；血压-收缩压/舒张压(60~260/40~200)，如120/80 |
| `remark` | string | 否 | 备注 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 体重健康趋势

**GET** `/appApi/manualHealthRecord/weight/trend`

查询体重趋势，包括BMI、目标体重、趋势图、统计和记录列表。range=day（当天24小时）/week（所在周周一~周日）/month（所在月每天），date=基准日期(yyyy-MM-dd)默认今天

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `range` | query | string | 否 | 时间范围：day/week/month，默认week |
| `date` | query | string | 否 | 基准日期(yyyy-MM-dd)，day默认今天/week默认当前周/month默认当前月 |
| `pageNum` | query | integer/int32 | 否 | 记录列表页码，默认1 |
| `pageSize` | query | integer/int32 | 否 | 记录列表每页条数，默认10 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 查询当前用户的手动健康数据记录列表

**GET** `/appApi/manualHealthRecord/list`

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 体温健康趋势

**GET** `/appApi/manualHealthRecord/bodyTemperature/trend`

查询体温趋势，包括安全范围、趋势图、统计和记录列表。range=day（当天24小时）/week（所在周周一~周日）/month（所在月每天），date=基准日期(yyyy-MM-dd)默认今天

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `range` | query | string | 否 | 时间范围：day/week/month，默认week |
| `date` | query | string | 否 | 基准日期(yyyy-MM-dd)，day默认今天/week默认当前周/month默认当前月 |
| `pageNum` | query | integer/int32 | 否 | 记录列表页码，默认1 |
| `pageSize` | query | integer/int32 | 否 | 记录列表每页条数，默认10 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 血糖健康趋势

**GET** `/appApi/manualHealthRecord/bloodSugar/trend`

查询血糖趋势，包括安全范围、趋势图、统计和记录列表。range=day（当天24小时）/week（所在周周一~周日）/month（所在月每天），date=基准日期(yyyy-MM-dd)默认今天

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `range` | query | string | 否 | 时间范围：day/week/month，默认week |
| `date` | query | string | 否 | 基准日期(yyyy-MM-dd)，day默认今天/week默认当前周/month默认当前月 |
| `pageNum` | query | integer/int32 | 否 | 记录列表页码，默认1 |
| `pageSize` | query | integer/int32 | 否 | 记录列表每页条数，默认10 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 血压健康趋势

**GET** `/appApi/manualHealthRecord/bloodPressure/trend`

查询血压趋势，包括安全范围、趋势图、统计和记录列表。range=day（当天24小时）/week（所在周周一~周日）/month（所在月每天），date=基准日期(yyyy-MM-dd)默认今天

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `range` | query | string | 否 | 时间范围：day/week/month，默认week |
| `date` | query | string | 否 | 基准日期(yyyy-MM-dd)，day默认今天/week默认当前周/month默认当前月 |
| `pageNum` | query | integer/int32 | 否 | 记录列表页码，默认1 |
| `pageSize` | query | integer/int32 | 否 | 记录列表每页条数，默认10 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## 健康档案

# 保存健康档案

**POST** `/appApi/healthArchive/save`

按 archiveType 整组提交 items[{optionId,name}]（手动输入项 optionId 传 null）；每次保存先删除该类型旧记录再全部新插，空列表表示清空该类型

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `archiveType` | integer/int32 | 是 | 档案类型(1既往病史 2家族病史 3手术史 4过敏史 5用药史 6个人史 7婚育史 8月经史 9运动史) |
| `items` | ItemDTO[] | 否 | 选项列表（勾选项+手动输入项，手动输入项 optionId 传 null；空列表表示清空该类型档案） |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 异常指标记录（周统计）

**GET** `/appApi/healthArchive/weekly`

传入日期返回该周数据，不传默认本周；days固定7条（周一~周日，count为命中器官项数），contents为本周有器官关联的档案内容列表

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `date` | query | string/date | 否 | 周内任意日期(yyyy-MM-dd)，不传默认今天 |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 器官分类列表

**GET** `/appApi/healthArchive/organs`

返回所有启用的器官分类（排除「无」），含分类ID、名称、icon（按当前用户性别返回男/女ICON）及当前用户该器官下的健康档案项items[{optionId,name}]

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 档案类型疾病列表

**GET** `/appApi/healthArchive/options`

传入档案类型(1-9)，返回该类型的选项配置：tags全量列表 + groups按器官分类分组

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `archiveType` | query | integer/int32 | 是 | 档案类型(1既往病史 2家族病史 3手术史 4过敏史 5用药史 6个人史 7婚育史 8月经史 9运动史) |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 查询当前用户指定档案类型的档案信息

**GET** `/appApi/healthArchive/detail`

按档案类型查询当前用户的档案；archiveType传0返回全部类型（9项，无数据的类型items为空）

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `archiveType` | query | integer/int32 | 是 | 档案类型(0全部 1既往病史 2家族病史 3手术史 4过敏史 5用药史 6个人史 7婚育史 8月经史 9运动史) |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 按器官查询档案内容

**GET** `/appApi/healthArchive/byOrgan`

传入器官分类ID，返回当前健康档案中属于该器官的选项内容，逗号分隔字符串

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `categoryId` | query | integer/int64 | 是 | 器官分类ID |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

## 用户设备

# 同步设备状态

**POST** `/appApi/device/syncStatus`

更新电量百分比、固件版本、最后同步时间

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `deviceMac` | string | 是 | 设备MAC地址 |
| `batteryLevel` | integer/int32 | 否 | 电量百分比(0-100) |
| `deviceVersion` | string | 否 | 固件版本 |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 绑定设备

**POST** `/appApi/device/bind`

## 请求参数

_无请求参数_

## 请求体

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `deviceMac` | string | 是 | 设备MAC地址 |
| `deviceName` | string | 否 | 设备名称(用户自定义) |
| `deviceModel` | string | 否 | 设备型号 |
| `deviceVersion` | string | 否 | 固件版本 |
| `batteryLevel` | integer/int32 | 否 | 电量百分比(0-100) |

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 设备详情

**GET** `/appApi/device/{bindId}`

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `bindId` | path | integer/int64 | 是 |  |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 我的设备列表

**GET** `/appApi/device/list`

## 请求参数

_无请求参数_

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---

# 解绑设备

**DELETE** `/appApi/device/unbind/{bindId}`

## 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `bindId` | path | integer/int64 | 是 |  |

## 请求体

_无请求体_

## 响应结构

| 状态码 | 说明 | Schema |
| --- | --- | --- |
| 200 | OK | object |

---
