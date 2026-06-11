# Spring AI Alibaba Agent Demo 实施计划

> **Goal:** 基于 Spring AI Alibaba Agent Framework + DashScope 构建 Agent Demo，与现有 langchain4j 方案并行共存。
>
> **Architecture:** Spring Boot 3.3.5 + DashScopeChatModel + ChatClient + Function Tools + InMemoryChatMemory + SimpleVectorStore。AgentObserver 提供全链路追踪。
>
> **Tech Stack:** Spring Boot 3.3.5, Spring AI 1.0.0-M6, spring-ai-alibaba-agent-framework 1.1.2.0, spring-ai-alibaba-starter-dashscope 1.1.2.1, JDK 17.
>
> **Package:** `com.llm.springai`

---

## 文件清单

| 文件 | 职责 |
|------|------|
| `pom.xml` | 添加 Spring Boot parent + Alibaba AI 依赖（保留 langchain4j 依赖不变） |
| `src/main/resources/application.yml` | DashScope 配置 |
| `springai/AlibabaAgentConfig.java` | Spring @Configuration，创建所有 Bean |
| `springai/AlibabaAgentService.java` | Agent 服务封装（ChatClient + Memory + 工具编排） |
| `springai/AgentObserver.java` | 可观测性（请求/响应/工具调用/Memory 快照） |
| `springai/tool/CalculatorTool.java` | 数学计算 Function |
| `springai/tool/SearchTool.java` | 搜索模拟 Function |
| `springai/tool/TimeQueryTool.java` | 时间查询 Function |
| `springai/memory/AgentMemoryManager.java` | 多用户 ChatMemory 管理 |
| `springai/rag/RagRetriever.java` | 向量检索 |
| `springai/rag/KnowledgeBaseInitializer.java` | 知识库初始化 |
| `springai/mcp/McpIntegration.java` | MCP 客户端 + 降级工具 |
| `springai/AlibabaAgentDemo.java` | 交互式终端入口 |
| `springai/AlibabaAgentBatchTest.java` | 批量集成测试 |

---

### Task 1: 更新 pom.xml 和项目配置

**Files:**
- Modify: `pom.xml`
- Create: `src/main/resources/application.yml`

- [ ] **Step 1: 更新 pom.xml** — 添加 Spring Boot parent、spring-ai-alibaba-agent-framework、spring-ai-alibaba-starter-dashscope 依赖，保留所有 langchain4j 依赖

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
</parent>

<repositories>
    <repository>
        <id>spring-milestones</id>
        <url>https://repo.spring.io/milestone</url>
    </repository>
</repositories>

<!-- 新增依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-agent-framework</artifactId>
    <version>1.1.2.0</version>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    <version>1.1.2.1</version>
</dependency>
```

- [ ] **Step 2: 创建 application.yml** — DashScope 模型配置

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
```

- [ ] **Step 3: 编译验证** `mvn compile -q`

---

### Task 2: 创建 Tool 类（Calculator / Search / TimeQuery）

**Files:**
- Create: `src/main/java/com/llm/springai/tool/CalculatorTool.java`
- Create: `src/main/java/com/llm/springai/tool/SearchTool.java`
- Create: `src/main/java/com/llm/springai/tool/TimeQueryTool.java`

- [ ] **Step 1: CalculatorTool** — 实现 `Function<CalculatorTool.Request, String>`，双栈表达式求值器（+、-、*、/、^、sqrt、sin、cos、abs 等）
- [ ] **Step 2: SearchTool** — 实现 `Function<SearchTool.Request, String>`，模拟搜索（返回预设摘要）
- [ ] **Step 3: TimeQueryTool** — 实现 `Function<TimeQueryTool.Request, String>`，支持 now/date/weekday/timestamp/timezone 查询
- [ ] **Step 4: 编译验证**

---

### Task 3: 创建 Memory 管理组件

**Files:**
- Create: `src/main/java/com/llm/springai/memory/AgentMemoryManager.java`

- [ ] **Step 1: AgentMemoryManager** — `ConcurrentHashMap<String, ChatMemory>` 多用户隔离，默认滑动窗口 20 条消息
- [ ] **Step 2: 编译验证**

---

### Task 4: 创建 RAG 管道（RagRetriever + KnowledgeBaseInitializer）

**Files:**
- Create: `src/main/java/com/llm/springai/rag/RagRetriever.java`
- Create: `src/main/java/com/llm/springai/rag/KnowledgeBaseInitializer.java`

- [ ] **Step 1: RagRetriever** — 封装 `SimpleVectorStore` + `EmbeddingModel`，提供 `search(query)` 方法
- [ ] **Step 2: KnowledgeBaseInitializer** — 启动时扫描 `knowledge/` 目录，加载 txt/md 文件，写入 VectorStore
- [ ] **Step 3: 编译验证**

---

### Task 5: 创建 MCP 集成组件

**Files:**
- Create: `src/main/java/com/llm/springai/mcp/McpIntegration.java`

- [ ] **Step 1: McpIntegration** — MCP 客户端连接管理 + WeatherTool / FileReaderTool 降级 Function beans
- [ ] **Step 2: 编译验证**

---

### Task 6: 创建 Agent 核心（Config + Service + Observer）

**Files:**
- Create: `src/main/java/com/llm/springai/AlibabaAgentConfig.java`
- Create: `src/main/java/com/llm/springai/AlibabaAgentService.java`
- Create: `src/main/java/com/llm/springai/AgentObserver.java`

- [ ] **Step 1: AlibabaAgentConfig** — `@Configuration`，创建 ChatClient Bean（装配所有 Tool + Advisor）
- [ ] **Step 2: AlibabaAgentService** — `@Service`，封装 ChatClient 调用，多用户隔离，Memory 管理
- [ ] **Step 3: AgentObserver** — 请求/响应/错误/工具调用的全链路追踪日志
- [ ] **Step 4: 编译验证**

---

### Task 7: 创建 Demo 和 BatchTest 入口

**Files:**
- Create: `src/main/java/com/llm/springai/AlibabaAgentDemo.java`
- Create: `src/main/java/com/llm/springai/AlibabaAgentBatchTest.java`

- [ ] **Step 1: AlibabaAgentDemo** — `@SpringBootApplication` + `CommandLineRunner`，交互式终端（/tools /mcp /rag /memory /clear /bye）
- [ ] **Step 2: AlibabaAgentBatchTest** — 自动化集成测试（验证天气查询 + 工具调用）
- [ ] **Step 3: 编译验证**

---

### Task 8: 整体编译与验证

- [ ] **Step 1: 完整编译** `mvn clean compile`
- [ ] **Step 2: 检查编译错误并修复**
- [ ] **Step 3: 运行 Demo** `DASHSCOPE_API_KEY=sk-xxx mvn exec:java -Dexec.mainClass="com.llm.springai.AlibabaAgentDemo"`
- [ ] **Step 4: 验证功能** — 对话、工具调用、Memory、RAG、MCP、Observer 输出
- [ ] **Step 5: Commit**
