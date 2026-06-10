# client 包架构文档

## 一句话概述

零框架依赖的大模型 API 客户端，仅用 JDK 11 自带的 `java.net.http`，实现了**多轮对话记忆**和 **Function Calling（工具调用）**，支持任意兼容 OpenAI 接口的 LLM（DeepSeek、通义千问等）。

---

## 文件地图（8 个文件，按阅读顺序）

| 文件 | 职责 | 一句话 |
|------|------|--------|
| `Tool.java` | 工具接口 | 定义工具的契约：名称、描述、参数 schema、执行方法 |
| `SearchTool.java` | 搜索工具 | 模拟搜索引擎（生产换真实 API） |
| `CalculatorTool.java` | 计算器 | 双栈算法求值，安全无注入 |
| `TimeQueryTool.java` | 时间查询 | 获取当前时间/日期/时区 |
| `ToolRegistry.java` | 工具注册中心 | 维护 名称→工具实例 映射，生成 JSON schema |
| `ChatMemory.java` | 对话记忆 | 滑动窗口，保留最近 N 轮，自动淘汰旧消息 |
| `LlmClient.java` | 核心客户端 | HTTP 通信、Function Calling 循环、可视化追踪 |
| `InteractiveChat.java` | 终端入口 | 交互式对话，DeepSeek 默认配置 |

---

## 核心架构

```
InteractiveChat (main 入口)
    │
    ├── ToolRegistry  ←── SearchTool, CalculatorTool, TimeQueryTool
    ├── ChatMemory    ←── 滑动窗口 (默认 10 轮)
    └── LlmClient
            │
            ├── sendMessage()          ← 高层入口（记忆 + 工具 全自动）
            │     ├── chatMemory.addUser()
            │     ├── chatWithToolsInternal()   ← Function Calling 循环
            │     │     └── buildRequestBody()   ← 手写 JSON 构造
            │     │     └── extractToolCalls()   ← 手写 JSON 解析
            │     │     └── tool.execute()       ← 本地执行
            │     └── chatMemory.addAssistant()
            │
            ├── chat()                 ← 底层调用（无工具）
            └── chatStream()           ← 流式输出
```

---

## Function Calling 完整流程

这是整个包最核心的逻辑，以用户输入 `234*456` 为例：

```
┌─────────────────────────────────────────────────────┐
│                    用户输入: 234*456                  │
└─────────────────────────────────────────────────────┘
                          │
                          ▼
          ┌──────────────────────────────┐
          │  ChatMemory 写入 user 消息    │
          │  窗口: 1/10 轮               │
          └──────────────────────────────┘
                          │
                          ▼
          ┌──────────────────────────────┐
          │  第 1 轮 API 请求             │
          │  POST /v1/chat/completions   │
          │  {                           │
          │    messages: [system, user], │
          │    tools: [搜索,计算器,时间]   │  ← 每次都全量带上
          │  }                           │
          └──────────────────────────────┘
                          │
                          ▼
          ┌──────────────────────────────┐
          │  DeepSeek 返回               │
          │  tool_calls: [               │
          │    calculator("234*456")     │
          │  ]                           │
          │  (没有文本，只有工具调用)      │
          └──────────────────────────────┘
                          │
                          ▼
          ┌──────────────────────────────┐
          │  extractToolCalls() 解析      │
          │  从 JSON 字符串中抠出:        │
          │    name = "calculator"       │
          │    arguments = "234*456"     │
          └──────────────────────────────┘
                          │
                          ▼
          ┌──────────────────────────────┐
          │  ToolRegistry.get() 查找      │
          │  → 找到 CalculatorTool       │
          │  CalculatorTool.execute()    │
          │  → 双栈算法算出 106704       │   ← 真正的计算在本地
          └──────────────────────────────┘
                          │
                          ▼
          ┌──────────────────────────────┐
          │  第 2 轮 API 请求             │
          │  messages: [                 │
          │    system,                   │
          │    user("234*456"),          │
          │    assistant(tool_calls),    │  ← 告诉 LLM 它调了什么
          │    tool("106704")             │  ← 工具执行结果
          │  ],                          │
          │  tools: [搜索,计算器,时间]     │  ← 工具列表照带
          └──────────────────────────────┘
                          │
                          ▼
          ┌──────────────────────────────┐
          │  DeepSeek 返回               │
          │  content: "234×456=106704"   │
          │  (没有 tool_calls，循环结束)  │
          └──────────────────────────────┘
                          │
                          ▼
          ┌──────────────────────────────┐
          │  ChatMemory 写入 assistant   │
          │  窗口: 2/10 轮               │
          │  返回给用户                   │
          └──────────────────────────────┘
```

---

## 三个关键方法的职责

### `sendMessage(String)` — 高层入口

```
1. 写入 ChatMemory（user 消息）
2. 从 ChatMemory 复制上下文消息列表
3. 调用 chatWithToolsInternal()（如果注册了工具）或 chat()
4. 写入 ChatMemory（assistant 回复）
5. 返回最终文本
```

### `chatWithToolsInternal(List<Message>)` — Function Calling 循环

```
while (round < 5) {
    1. buildRequestBody()     组装 JSON（消息 + 工具列表）
    2. HTTP POST 到 LLM
    3. extractToolCalls()     解析响应中有没有 tool_calls
    4. 如果有 tool_calls:
         → 执行工具（本地 Java 方法调用）
         → 结果追加到消息列表
         → continue（回到 while 顶部，round+1）
    5. 如果没有 tool_calls:
         → 提取文本内容
         → return（循环结束）
}
throw 异常（5 轮都没拿到文本，触发熔断）
```

**循环退出条件只有两个**：
- LLM 返回了纯文本（正常退出）
- 超过 `maxToolCallRounds` 默认 5 次（异常退出）

### `buildRequestBody()` — JSON 序列化

整个项目没有任何 JSON 库（不用 Jackson/Gson），所有 JSON 手写 StringBuilder 拼接。关键点：

- 每条 Message 支持 `role`、`content`、`tool_calls`、`tool_call_id`、`name`
- 工具列表每次都全量拼入 `"tools":[...]`
- `escapeJson()` 处理 `"` `\` `\n` `\r` `\t` 转义

---

## ChatMemory 滑动窗口机制

```
轮次计数规则: 一条 user 消息 = 1 轮

淘汰逻辑:
  当 currentRounds > maxRounds 时:
    1. 找到最早一条 user 消息
    2. 删除它 + 紧随它的 assistant + tool 消息（完整删除一轮）
    3. 重复直到轮数 ≤ 上限

system 消息 → 始终保留，不计入轮次
tool 消息   → 跟随所在轮次一起淘汰
```

---

## Tool 接口设计

```java
interface Tool {
    String getName();                   // 工具名，LLM 通过它决定调用谁
    String getDescription();            // 功能描述，LLM 判断何时使用
    String getParametersJsonSchema();   // 参数 JSON Schema
    String execute(String argsJson);    // 真正的执行逻辑
}
```

注册方式：`ToolRegistry` 维护 `Map<String, Tool>`，链式注册：

```java
new ToolRegistry()
    .register(new SearchTool())
    .register(new CalculatorTool())
    .register(new TimeQueryTool());
```

---

## 可视化追踪

默认开启，输入 `/trace` 切换。每次对话显示：

```
╔══════════════════════════════════════════╗
║  用户输入: 234*456                       ║
╚══════════════════════════════════════════╝
  📝 记忆写入: user → 234*456
  📊 记忆窗口: 1 / 10 轮

  ┌─ 第 1 轮: 发送 2 条消息到 LLM ─
  │ ⚙️  [system] 你是一个有用的助手...
  │ 👤 [user] 234*456
  │ 🧰 可用工具: search, calculator, time_query
  └──────────────────────────────────────────
  ╭─ 🔧 LLM 调用工具: calculator
  │   参数: {"expression":"234*456"}
  ╰──────────────────────────
     ✔ calculator 执行完毕 → 234*456 = 106704
     📝 记忆写入: assistant(tool_calls) → calculator
     📝 记忆写入: tool(calculator) → 234*456 = 106704

  ┌─ 第 2 轮: 发送 4 条消息到 LLM ─
  │ ⚙️  [system] ...
  │ 👤 [user] 234*456
  │ 🤖 [assistant → 调用 calculator] (tool_calls)
  │ 🔧 [tool → 返回 calculator 结果] 234*456 = 106704
  │ 🧰 可用工具: search, calculator, time_query
  └──────────────────────────────────────────
     ✅ LLM 直接回复（无需再调用工具）
  📝 记忆写入: assistant → 234×456=106704
╔══════════════════════════════════════════╗
║  AI 回复: 234 × 456 = 106704            ║
╚══════════════════════════════════════════╝
```

---

## 设计决策与取舍

| 决策 | 原因 | 代价 |
|------|------|------|
| 零依赖 | 只需 JDK 11+，不用 Maven/Gradle | 手写 JSON，大项目建议引入 JSON 库 |
| 工具全量传输 | 实现简单，3 个工具开销可忽略 | 工具多时浪费 token |
| 滑动窗口按轮淘汰 | 保证对话上下文完整 | 不区分消息重要性 |
| maxToolCallRounds=5 | 防止死循环打穿费用 | 复杂任务可能不够 |
| 手写双栈计算器 | 安全，无 ScriptEngine 注入风险 | 不支持复杂数学函数 |
| 搜索工具为模拟 | 演示 function calling 流程 | 生产需替换真实搜索 API |

---

## 快速开始

```bash
# 1. 设置 API Key
export DEEPSEEK_API_KEY=sk-your-key

# 2. 编译
javac -d out src/main/java/com/llm/client/*.java

# 3. 运行
java -cp out com.llm.client.InteractiveChat
```

终端命令：`/help` `/tools` `/memory` `/trace` `/debug` `/clear` `/bye`