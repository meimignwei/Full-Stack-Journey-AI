# Full-Stack-Journey-AI

全栈 AI 学习项目，从零开始构建 LLM 客户端，逐步演进到 LangChain4j Agent 框架。

## 项目概览

```
full-stack-journey-ai/
├── knowledge/                          # RAG 知识库（.txt/.md 文件）
│   └── fullstack-ai-notes.txt
├── doc/                                # 设计文档与实现计划
│   ├── client-package.md
│   └── superpowers/
│       ├── plans/
│       └── specs/
└── src/main/java/com/llm/
    ├── client/                         # 纯 JDK 原生 LLM 客户端（零依赖）
    │   ├── LlmClient.java              #   HTTP 调用、SSE 流式、Function Calling 循环
    │   ├── LlmClientDemo.java          #   程序化调用示例
    │   ├── InteractiveChat.java        #   交互式终端
    │   ├── Tool.java                   #   工具抽象接口
    │   ├── ToolRegistry.java           #   工具注册、JSON Schema 生成
    │   ├── CalculatorTool.java         #   表达式计算器（双栈算法）
    │   ├── SearchTool.java             #   模拟搜索
    │   ├── TimeQueryTool.java          #   时间查询
    │   └── ChatMemory.java             #   滑动窗口记忆
    │
    └── langchain4j/                    # LangChain4j Agent（框架实现）
        ├── LangChain4jAgentDemo.java   #   交互式 Agent 终端入口
        ├── AgentService.java           #   AI Service 接口
        ├── AgentConfig.java            #   装配工厂（组装所有组件）
        ├── AgentObserver.java          #   可观测性（Token 统计、调用追踪）
        ├── tool/                       #   @Tool 工具集
        │   ├── CalculatorTool.java     #     数学计算
        │   ├── SearchTool.java         #     模拟搜索
        │   ├── TimeQueryTool.java      #     时间查询
        │   └── RagTool.java            #     RAG 知识库检索
        ├── memory/
        │   └── AgentMemoryProvider.java #   聊天记忆管理
        ├── rag/                        #   RAG 管道
        │   ├── RagRetriever.java       #     向量检索
        │   ├── KnowledgeBaseInitializer.java # 文档摄取
        │   └── RagTest.java            #     RAG 独立测试
        └── mcp/                        #   MCP 集成
            ├── McpToolProvider.java    #     MCP 客户端（含降级工具）
            └── DemoMcpServer.java      #     MCP 服务端占位
```

## 两套实现对比

| 特性 | `com.llm.client` | `com.llm.langchain4j` |
|------|-----------------|----------------------|
| 依赖 | 零依赖，纯 JDK 11+ | LangChain4j 1.0.0-beta3 |
| HTTP | `java.net.http.HttpClient` | OkHttp（框架内置） |
| JSON | 手动 StringBuilder 构建 | Jackson（框架内置） |
| Function Calling | 手写循环 + 反射调用 | `@Tool` 注解，框架自动 |
| 流式 | SSE 手动解析 | 框架自动处理 |
| 记忆 | 简易 ChatMemory | MessageWindowChatMemory |
| RAG | 无 | InMemoryEmbeddingStore + BGE 本地模型 |
| MCP | 无 | ✅ 支持（含降级模式） |
| 可观测性 | 无 | ChatModelListener 钩子 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- DeepSeek API Key（[获取地址](https://platform.deepseek.com)）

### 运行 LangChain4j Agent

```bash
export DEEPSEEK_API_KEY=sk-your-key-here
mvn compile exec:java
```

终端交互命令：

| 命令 | 功能 |
|------|------|
| `/tools` | 查看已注册的工具列表 |
| `/mcp` | 查看 MCP 连接状态 |
| `/rag` | 查看 RAG 知识库状态 |
| `/memory` | 查看对话记忆快照 |
| `/clear` | 清空对话记忆 |
| `/trace` | 查看最近 LLM 调用追踪 |
| `/bye` | 退出 |

### 运行 RAG 测试

```bash
mvn compile exec:java -Dexec.mainClass="com.llm.langchain4j.rag.RagTest"
```

无需 API Key，使用本地 BGE-small-zh-v1.5 模型进行向量检索。

### 运行原生客户端

```bash
mvn compile exec:java -Dexec.mainClass="com.llm.client.InteractiveChat"
```

## 架构设计

### Agent 工作流

```
用户输入 → AgentService.chat()
    │
    ▼
AiServices 动态代理
    │
    ├── SystemMessage + 历史消息 + 用户消息
    ├── 所有 @Tool 的 JSON Schema
    │
    ▼
LLM (DeepSeek Chat)
    │
    ├── 文本回复 → 直接返回
    └── tool_calls →
        ├── 反射调用对应 @Tool 方法
        ├── 结果注入对话上下文
        └── 再次请求 LLM（循环直到文本回复）
```

### RAG 管道

```
knowledge/*.txt
    → FileSystemDocumentLoader（文档加载）
    → DocumentSplitters.recursive(500, 50)（分割）
    → EmbeddingStoreIngestor（向量化）
    → InMemoryEmbeddingStore（存储）
    ↓
用户查询 → 生成查询向量 → 相似度搜索（top-3） → 返回片段
```

### MCP 降级模式

当 MCP 原生连接不可用时，自动切换为内置降级工具（`get_weather`、`read_local_file`），确保 Agent 始终可用。

## 技术栈

| 组件 | 技术 |
|------|------|
| LLM | DeepSeek Chat（OpenAI 兼容 API） |
| Embedding | BGE-small-zh-v1.5（本地 ONNX 推理） |
| Agent 框架 | LangChain4j 1.0.0-beta3 |
| 向量存储 | InMemoryEmbeddingStore |
| 文档解析 | Apache Tika |
| 日志 | SLF4J Simple |
