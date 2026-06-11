# Spring AI Alibaba Agent Demo 设计文档

## 概述

基于 Spring AI Alibaba Agent Framework + DashScope 模型，构建功能完整的 Agent Demo，与现有的 `com.llm.langchain4j` 方案并行共存。Demo 以 Spring Boot 应用 + 终端交互式对话形式运行，集成 Tool、Memory、RAG、MCP 四大能力及可观测性。

## 决策记录

- **LLM Provider**: 阿里云 DashScope（通义千问），通过 `spring-ai-alibaba-starter-dashscope` 自动配置
- **Agent 框架**: `spring-ai-alibaba-agent-framework` 1.1.2.0（构建 Agent、管理 Tool/Memory）
- **构建工具**: Maven + Spring Boot 3.3.5
- **Tool 定义**: Spring AI `Function<Input, Output>` + `ChatClient.defaultTools()`
- **交互方式**: Spring Boot `CommandLineRunner` 终端交互式对话
- **RAG**: DashScope Embedding → SimpleVectorStore
- **MCP**: 内置 MCP Client + 降级工具（Spring AI MCP 抽象）
- **Memory**: Spring AI `InMemoryChatMemory` 滑动窗口
- **包名**: `com.llm.springai`（与 `client`、`langchain4j` 同级，三者独立共存）

## 架构对照

| 能力 | langchain4j 方案 | Spring AI Alibaba 方案 |
|------|-----------------|----------------------|
| LLM | `OpenAiChatModel` → DeepSeek | `DashScopeChatModel` → 通义千问 |
| Embedding | `BgeSmallZhV15QuantizedEmbeddingModel` (本地) | `DashScopeEmbeddingModel` (云端) |
| Agent 构建 | `AiServices.builder(AgentService.class)` | `ChatClient.builder(chatModel).defaultTools(...)` |
| Tool 注册 | `@Tool` 注解 + `.tools(obj)` | `Function<Input, Output>` + `.defaultTools(...)` |
| Memory | `MessageWindowChatMemory` + `ChatMemoryProvider` | `InMemoryChatMemory` + `MessageChatMemoryAdvisor` |
| RAG | `InMemoryEmbeddingStore<TextSegment>` | `SimpleVectorStore` |
| MCP | `DefaultMcpClient` + stdio transport | Spring AI MCP client + 降级 Function beans |
| 可观测性 | `ChatModelListener` | `AgentObserver` + `SimpleLoggerAdvisor` |
| 启动方式 | `public static void main` 手动装配 | Spring Boot `CommandLineRunner` 自动注入 |

## 项目结构

```
src/main/java/com/llm/springai/
├── AlibabaAgentDemo.java          # 交互式终端入口（Spring Boot + CommandLineRunner）
├── AlibabaAgentBatchTest.java     # 批量集成测试
├── AlibabaAgentConfig.java        # Spring @Configuration Bean 装配
├── AlibabaAgentService.java       # Agent 服务封装（ChatClient + Memory + 工具编排）
├── AgentObserver.java             # 可观测性（请求/响应/工具调用/Memory 快照）
├── tool/                          # Spring AI Function 工具
│   ├── CalculatorTool.java        # 数学计算（双栈表达式求值）
│   ├── SearchTool.java            # 搜索模拟
│   └── TimeQueryTool.java         # 时间/日期/时区查询
├── memory/
│   └── AgentMemoryManager.java    # 多用户 ChatMemory 管理（ConcurrentHashMap）
├── rag/
│   ├── KnowledgeBaseInitializer.java  # 知识库文档加载/向量化
│   └── RagRetriever.java         # 向量检索接口
└── mcp/
    └── McpIntegration.java        # MCP 客户端 + 降级工具（WeatherTool / FileReaderTool）
```

## 核心组件

### AlibabaAgentConfig (@Configuration)

Spring 配置类，负责创建所有 Bean：
- `DashScopeChatModel`（由 starter 自动配置，读取 `DASHSCOPE_API_KEY`）
- `ChatClient`：装配 `defaultTools`（calculator, search, timeQuery, weather, fileReader）+ `SimpleLoggerAdvisor`
- `AgentMemoryManager`：滑动窗口记忆管理（默认 20 条消息）
- `RagRetriever` + `KnowledgeBaseInitializer`：RAG 管道
- MCP 降级工具 Bean：`WeatherTool`、`FileReaderTool`

### AlibabaAgentService (@Service)

封装 `ChatClient` 调用，提供统一接口：
```java
String chat(String userId, String message)
void clear(String userId)
```
内部使用 `MessageChatMemoryAdvisor` + `CHAT_MEMORY_CONVERSATION_ID_KEY` 实现多用户隔离。

### Tool 定义

每个 Tool 实现 `java.util.function.Function<Request, String>`，Request 为 `record`：

| Tool | Input record | 功能 |
|------|-------------|------|
| CalculatorTool | `expression: String` | 四则运算/sqrt/sin/cos/log/幂 |
| SearchTool | `query: String` | 模拟网络搜索 |
| TimeQueryTool | `query: String` | 时间/日期/时区查询 |
| WeatherTool | `city: String` | 模拟天气（MCP 降级） |
| FileReaderTool | `path: String` | 本地文件读取（MCP 降级） |

### RAG 管道

- `KnowledgeBaseInitializer`：启动时扫描 `knowledge/` 目录 → 加载 txt/md 文件 → 写入 `SimpleVectorStore`
- `RagRetriever`：接收查询 → DashScope Embedding 向量化 → `VectorStore.similaritySearch()` → 返回 Top-K 片段

### MCP 集成

- `McpIntegration`：尝试连接 MCP server→ 可用时注册真实 MCP 工具 → 不可用时降级为内置 `Function` beans
- 降级工具：`WeatherTool`（模拟天气）、`FileReaderTool`（本地文件读取）

### 可观测性

`AgentObserver` 追踪每次 Agent 调用的完整链路：
1. **请求阶段**：打印 userId + 用户消息
2. **响应阶段**：打印 LLM 回复
3. **错误阶段**：打印错误信息
4. **工具调用**：onToolCall() 记录工具名和结果
5. **Memory 快照**：显示当前消息数

### 终端命令

| 命令 | 功能 |
|------|------|
| `/tools` | 查看已注册工具和模型信息 |
| `/mcp` | 查看 MCP 连接状态（真实/降级） |
| `/rag` | 查看 RAG 知识库状态 |
| `/memory` | 查看当前对话记忆 |
| `/clear` | 清空对话记忆 |
| `/bye` | 退出 |
| `/help` | 帮助 |

## 配置

```yaml
# src/main/resources/application.yml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
```

环境变量：`export DASHSCOPE_API_KEY=sk-your-alibaba-key`

## 运行方式

```bash
# 交互式 Demo
mvn compile exec:java -Dexec.mainClass="com.llm.springai.AlibabaAgentDemo"

# 批量测试
mvn compile exec:java -Dexec.mainClass="com.llm.springai.AlibabaAgentBatchTest"
```

## 边界说明

- 与 `com.llm.client`（零依赖 HttpClient）和 `com.llm.langchain4j`（langchain4j Agent）互不依赖，三类Agent 独立共存
- MCP 集成优先尝试真实 MCP 服务连接，失败自动降级为内置 Function tools
- Embedding 模型使用 DashScope 云端服务（需 API Key），不依赖本地模型文件
- 引入 Spring Boot parent 后，编译插件配置由 parent 管理
