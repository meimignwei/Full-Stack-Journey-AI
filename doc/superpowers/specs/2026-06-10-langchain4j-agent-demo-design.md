# LangChain4j Agent Demo 设计文档

## 概述

基于 LangChain4j 构建一个功能完整的 Agent Demo，替代当前手工实现的 LLM 客户端。Demo 以终端交互式对话形式运行，集成 Tool/Skill、Memory、RAG、MCP 四大能力，并提供全程可观测性。

## 决策记录

- **LLM Provider**: DeepSeek API（通过 LangChain4j OpenAI 兼容接口）
- **构建工具**: Maven
- **Skill 定义**: LangChain4j `@Tool` 注解方法
- **交互方式**: 终端交互式对话
- **RAG**: 本地文件（txt/md）→ InMemoryEmbeddingStore
- **MCP**: 内置 Demo MCP Server + 可配置外部 MCP Server

## 项目结构

```
src/main/java/com/llm/langchain4j/
├── LangChain4jAgentDemo.java      # 交互式终端入口
├── AgentService.java              # AI Service 接口
├── AgentConfig.java               # 装配工厂（创建所有组件）
├── AgentObserver.java             # 可观测性监听器
├── skill/                         # 内置 @Tool Skill
│   ├── CalculatorSkill.java
│   ├── SearchSkill.java
│   ├── TimeQuerySkill.java
│   └── RagSkill.java
├── memory/
│   └── AgentMemoryProvider.java   # ChatMemoryProvider 实现
├── rag/
│   ├── KnowledgeBaseInitializer.java
│   └── RagRetriever.java
└── mcp/
    ├── DemoMcpServer.java          # 内置 MCP Server
    └── McpToolProvider.java        # MCP Client 管理
```

## 架构

### Agent 核心

- **AgentService**: LangChain4j AI Service 接口，`@SystemMessage` 设置角色
- **AgentConfig**: 手动装配 `OpenAiChatModel`（指向 DeepSeek）、`ChatMemoryProvider`、内置 Tool + MCP Tool，通过 `AiServices.builder()` 构建 AgentService 实例
- **ChatMemoryProvider**: 按 userId 提供 `MessageWindowChatMemory`（滑动窗口 10 轮）

### Skills（@Tool）

| Tool | 功能 |
|------|------|
| calculator | 四则运算 + sqrt |
| search | 搜索模拟 |
| timeQuery | 日期/星期/时间 |
| ragSearch | RAG 知识库检索 |

### RAG

- 启动时扫描 `knowledge/` 目录，加载 txt/md 文件
- 使用 DeepSeek Embedding API 向量化
- 存入 `InMemoryEmbeddingStore`
- `RagSkill` 作为 @Tool，Agent 自动决定何时检索知识库

### MCP

- **DemoMcpServer**: 内置 MCP Server（stdio transport），提供 `get_weather`（模拟天气）、`read_local_file` 工具
- **McpToolProvider**: 管理 MCP Client 生命周期，将 MCP 工具注入 Agent
- 可选配置连接外部 MCP Server

### 可观测性（AgentObserver）

实现 LangChain4j `ChatModelListener` 接口，追踪每个请求的完整链路：

1. **请求阶段**: 打印发送给 LLM 的消息列表、可用工具
2. **LLM 决策阶段**: 显示 LLM 是否决定调用工具、调用了哪个工具、为什么
3. **工具执行阶段**: 显示工具名称、参数、返回结果（MCP 工具标注 `[MCP]` 前缀）
4. **回复阶段**: 显示最终回复
5. **Memory 快照**: 每轮对话后显示窗口大小、消息数

输出示例：
```
╔══ Agent 请求 ── [第 1 轮]
║  ┌ 消息 [user]: 帮我查一下 AI 相关的知识，然后算一下 123*456
║  ├ 可用工具: [calculator, search, timeQuery, ragSearch] + MCP[get_weather, read_local_file]
║  └────────────────
║  LLM 决定调用工具: ragSearch("AI 相关")
║  [RAG] 检索: "AI 相关" → 命中 3 个文档片段
║  LLM 决定调用工具: calculator("123*456")
║  [Tool] calculator("123*456") → "56088"
║  LLM 最终回复: "根据知识库..."
║  📊 Memory 状态: 3 轮 / 最多 10 轮, 共 8 条消息
╚══
```

### 终端命令

| 命令 | 功能 |
|------|------|
| `/tools` | 查看所有可用工具（含 MCP 工具） |
| `/mcp` | 查看 MCP 连接状态和 MCP 专属工具 |
| `/rag` | 查看 RAG 知识库状态（文档数、片段数） |
| `/memory` | 查看当前对话记忆 |
| `/trace` | 切换可观测性详细程度 |
| `/clear` | 清空对话记忆 |
| `/bye` | 退出 |

## 边界说明

- 嵌入模型：DeepSeek 支持 embedding API，若不支持则降级为内存关键词匹配
- MCP transport：内置 Demo 使用 stdio，外部 MCP 可通过环境变量配置
- 不引入 Spring Boot，保持纯 Java main 启动
- 现有 `com.llm.client` 包保留不动