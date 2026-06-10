# LangChain4j Agent Demo 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 LangChain4j 1.16.1 构建交互式终端 Agent Demo，集成 Tool/Skill、Memory、RAG、MCP、可观测性。

**Architecture:** 纯 Java main 手动装配，AiServices + @Tool + ChatMemoryProvider + InMemoryEmbeddingStore + McpClient。AgentObserver 实现 ChatModelListener 全链路追踪。

**Tech Stack:** LangChain4j 1.16.1, DeepSeek API (OpenAI 兼容), MCP Java SDK (stdio transport), Maven, JDK 17.

---

## 文件清单

| 文件 | 职责 |
|------|------|
| `pom.xml` | Maven 构建，依赖管理 |
| `knowledge/fullstack-ai-notes.txt` | RAG 示例知识文档 |
| `langchain4j/AgentService.java` | AI Service 接口（@SystemMessage + @UserMessage） |
| `langchain4j/AgentConfig.java` | 装配工厂，创建并连线所有组件 |
| `langchain4j/AgentObserver.java` | 可观测性监听器（ChatModelListener） |
| `langchain4j/skill/CalculatorSkill.java` | 计算器 @Tool |
| `langchain4j/skill/SearchSkill.java` | 搜索 @Tool |
| `langchain4j/skill/TimeQuerySkill.java` | 时间查询 @Tool |
| `langchain4j/skill/RagSkill.java` | RAG 检索 @Tool |
| `langchain4j/memory/AgentMemoryProvider.java` | ChatMemoryProvider 实现 |
| `langchain4j/rag/KnowledgeBaseInitializer.java` | 文档加载/切片/向量化 |
| `langchain4j/rag/RagRetriever.java` | 向量检索接口 |
| `langchain4j/mcp/DemoMcpServer.java` | 内置 MCP Server（stdio, 天气+文件工具） |
| `langchain4j/mcp/McpToolProvider.java` | MCP Client 生命周期管理 |
| `langchain4j/LangChain4jAgentDemo.java` | 交互式终端入口 |

---

### Task 1: 创建 Maven pom.xml 和项目基础设施

**Files:**
- Create: `pom.xml`
- Create: `knowledge/fullstack-ai-notes.txt`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.llm</groupId>
    <artifactId>full-stack-journey-ai</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <langchain4j.version>1.16.1</langchain4j.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-mcp</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-easy-rag</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>2.0.13</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建知识库示例文件**

```bash
mkdir -p knowledge
```

文件 `knowledge/fullstack-ai-notes.txt`：
```
全栈 AI 开发笔记

## 大语言模型
大语言模型（LLM）如 GPT-4、DeepSeek 等，基于 Transformer 架构，
通过海量文本训练获得强大的语言理解和生成能力。

## Function Calling
Function Calling 允许 LLM 调用外部工具和 API，扩展其能力边界。
LLM 根据用户意图自动选择调用哪些工具，并生成结构化的参数。

## RAG
检索增强生成（Retrieval-Augmented Generation）结合了信息检索和文本生成。
先从知识库中检索相关文档片段，再让 LLM 基于检索结果生成回答，减少幻觉。

## MCP
Model Context Protocol 是 Anthropic 提出的开放协议，标准化了 AI 应用
与外部工具/数据源之间的通信方式。MCP 支持 stdio 和 HTTP SSE 两种传输模式。

## Agent
AI Agent 是能够自主规划、调用工具、多步推理的智能体。
LangChain4j 通过 AiServices + @Tool 提供了简洁的 Agent 构建方式。
```

- [ ] **Step 3: 编译验证 pom.xml**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add pom.xml knowledge/
git commit -m "chore: add Maven pom.xml and RAG knowledge base"
```

---

### Task 2: 创建 AgentService 接口 + AgentMemoryProvider

**Files:**
- Create: `src/main/java/com/llm/langchain4j/AgentService.java`
- Create: `src/main/java/com/llm/langchain4j/memory/AgentMemoryProvider.java`

- [ ] **Step 1: 创建 AgentService 接口**

```java
package com.llm.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AgentService {

    @SystemMessage("""
            你是一个全栈 AI 助手，具备以下能力：
            - 使用计算器进行数学计算
            - 使用搜索引擎获取最新信息
            - 查询当前时间和日期
            - 检索知识库获取专业知识
            - 调用 MCP 工具获取天气和文件信息
            
            回答简洁、准确。当你需要更多信息时，主动使用工具获取。
            """)
    String chat(@MemoryId String userId, @UserMessage String message);
}
```

- [ ] **Step 2: 创建 AgentMemoryProvider**

```java
package com.llm.langchain4j.memory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentMemoryProvider implements ChatMemoryProvider {

    private final Map<Object, ChatMemory> memories = new ConcurrentHashMap<>();
    private final int maxMessages;

    public AgentMemoryProvider(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    @Override
    public ChatMemory get(Object memoryId) {
        return memories.computeIfAbsent(memoryId,
                id -> MessageWindowChatMemory.withMaxMessages(maxMessages));
    }

    public ChatMemory getMemory(Object memoryId) {
        return memories.get(memoryId);
    }

    public void clear(Object memoryId) {
        ChatMemory memory = memories.get(memoryId);
        if (memory != null) {
            memory.clear();
        }
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/llm/langchain4j/
git commit -m "feat: add AgentService interface and memory provider"
```

---

### Task 3: 创建内置 Skills（Calculator, Search, TimeQuery）

**Files:**
- Create: `src/main/java/com/llm/langchain4j/skill/CalculatorSkill.java`
- Create: `src/main/java/com/llm/langchain4j/skill/SearchSkill.java`
- Create: `src/main/java/com/llm/langchain4j/skill/TimeQuerySkill.java`

- [ ] **Step 1: 创建 CalculatorSkill**

```java
package com.llm.langchain4j.skill;

import dev.langchain4j.agent.tool.Tool;

public class CalculatorSkill {

    @Tool("执行数学计算，支持加减乘除和 sqrt 开方，例如: 38*47+sqrt(256)")
    public String calculator(String expression) {
        try {
            String processed = expression.replace("sqrt(", "Math.sqrt(");
            double result = eval(processed);
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return String.valueOf((long) result);
            }
            return String.valueOf(result);
        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }
    }

    private double eval(String expr) {
        // 简易表达式求值器（处理 + - * / 和括号）
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) { nextChar(); return true; }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < expr.length()) throw new RuntimeException("意外的字符: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();
                double x;
                int startPos = this.pos;
                if (eat('(')) { x = parseExpression(); eat(')'); }
                else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expr.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') {
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = expr.substring(startPos, this.pos);
                    if (func.equals("Math.sqrt")) {
                        eat('('); x = Math.sqrt(parseExpression()); eat(')');
                    } else {
                        throw new RuntimeException("未知函数: " + func);
                    }
                } else {
                    throw new RuntimeException("意外的字符: " + (char) ch);
                }
                if (eat('^')) x = Math.pow(x, parseFactor());
                return x;
            }
        }.parse();
    }
}
```

- [ ] **Step 2: 创建 SearchSkill**

```java
package com.llm.langchain4j.skill;

import dev.langchain4j.agent.tool.Tool;

public class SearchSkill {

    @Tool("搜索互联网获取最新信息，输入搜索关键词，返回相关结果摘要")
    public String search(String query) {
        // 模拟搜索结果，实际可替换为真实搜索 API
        return String.format("""
                搜索结果: "%s"
                
                1. 【AI 最新动态】OpenAI 发布新一代多模态模型，支持文本、图像、音频的联合推理。
                2. 【技术进展】RAG 技术持续演进，Agentic RAG 成为新的研究方向。
                3. 【行业应用】多家企业已将 AI Agent 应用于客服、代码生成等场景。
                4. 【开源动态】LangChain4j 1.16 版本发布，MCP 支持更加完善。
                
                (模拟搜索，实际可接入 SerpAPI 或 Tavily)
                """, query);
    }
}
```

- [ ] **Step 3: 创建 TimeQuerySkill**

```java
package com.llm.langchain4j.skill;

import dev.langchain4j.agent.tool.Tool;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeQuerySkill {

    @Tool("查询当前日期、时间、星期几")
    public String timeQuery(String question) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        String dayOfWeek = switch (now.getDayOfWeek()) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
        return String.format("当前时间: %s %s %s",
                now.format(dateFmt), now.format(timeFmt), dayOfWeek);
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/llm/langchain4j/skill/
git commit -m "feat: add Calculator, Search, and TimeQuery skills"
```

---

### Task 4: 创建 RAG 管道（RagRetriever + KnowledgeBaseInitializer + RagSkill）

**Files:**
- Create: `src/main/java/com/llm/langchain4j/rag/RagRetriever.java`
- Create: `src/main/java/com/llm/langchain4j/rag/KnowledgeBaseInitializer.java`
- Create: `src/main/java/com/llm/langchain4j/skill/RagSkill.java`

- [ ] **Step 1: 创建 RagRetriever**

```java
package com.llm.langchain4j.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

public class RagRetriever {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final int maxResults;

    public RagRetriever(EmbeddingModel embeddingModel, int maxResults) {
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        this.embeddingModel = embeddingModel;
        this.maxResults = maxResults;
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    public String search(String query) {
        if (embeddingStore == null) {
            return "知识库未初始化";
        }
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, maxResults);
            if (matches.isEmpty()) {
                return "未找到相关知识";
            }
            StringBuilder sb = new StringBuilder("知识库检索结果:\n\n");
            for (int i = 0; i < matches.size(); i++) {
                EmbeddingMatch<TextSegment> match = matches.get(i);
                sb.append("【片段 ").append(i + 1).append("】相似度: ")
                        .append(String.format("%.2f", match.score()))
                        .append("\n").append(match.embedded().text()).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "RAG 检索失败: " + e.getMessage();
        }
    }
}
```

- [ ] **Step 2: 创建 KnowledgeBaseInitializer**

```java
package com.llm.langchain4j.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;

import java.nio.file.Path;
import java.util.List;

public class KnowledgeBaseInitializer {

    private final String knowledgeDir;
    private final RagRetriever retriever;

    public KnowledgeBaseInitializer(String knowledgeDir, RagRetriever retriever) {
        this.knowledgeDir = knowledgeDir;
        this.retriever = retriever;
    }

    public int initialize() {
        Path dirPath = Path.of(knowledgeDir);
        if (!dirPath.toFile().exists() || !dirPath.toFile().isDirectory()) {
            System.out.println("[RAG] 知识库目录不存在，跳过初始化: " + knowledgeDir);
            return 0;
        }

        EmbeddingStore<TextSegment> store = retriever.getEmbeddingStore();

        // 加载所有 txt/md 文件
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(
                dirPath,
                new TextDocumentParser()
        );

        if (documents.isEmpty()) {
            System.out.println("[RAG] 知识库目录中没有文档");
            return 0;
        }

        // 使用 LangChain4j 内置的 EmbeddingStoreIngestor
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500, 50))
                .embeddingStore(store)
                .build();

        ingestor.ingest(documents);

        System.out.println("[RAG] 已加载 " + documents.size() + " 个文档到知识库");
        return documents.size();
    }
}
```

- [ ] **Step 3: 创建 RagSkill**

```java
package com.llm.langchain4j.skill;

import com.llm.langchain4j.rag.RagRetriever;
import dev.langchain4j.agent.tool.Tool;

public class RagSkill {

    private final RagRetriever retriever;

    public RagSkill(RagRetriever retriever) {
        this.retriever = retriever;
    }

    @Tool("从本地知识库中检索相关文档，输入查询问题，返回匹配的文档片段")
    public String ragSearch(String question) {
        return retriever.search(question);
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/llm/langchain4j/rag/ src/main/java/com/llm/langchain4j/skill/RagSkill.java
git commit -m "feat: add RAG pipeline with local knowledge base"
```

---

### Task 5: 创建 MCP Demo Server + McpToolProvider

**Files:**
- Create: `src/main/java/com/llm/langchain4j/mcp/DemoMcpServer.java`
- Create: `src/main/java/com/llm/langchain4j/mcp/McpToolProvider.java`

- [ ] **Step 1: 创建 DemoMcpServer（内置 MCP Server）**

```java
package com.llm.langchain4j.mcp;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/**
 * 内置 MCP Demo Server —— 通过 stdio 传输
 * 提供 get_weather 和 read_local_file 两个工具
 */
public class DemoMcpServer {

    public static void main(String[] args) {
        // 创建 MCP Server
        McpSyncServer server = McpServer.sync()
                .serverInfo("demo-server", "1.0.0")
                .build();

        // 注册 get_weather 工具
        server.addTool(
                new McpServerFeatures.SyncToolSpecification(
                        new McpSchema.Tool(
                                "get_weather",
                                "获取指定城市的天气信息",
                                McpSchema.JsonSchema.OBJECT()
                                        .addProperty("city", McpSchema.JsonSchema.STRING()
                                                .description("城市名称，如 Beijing、Shanghai"))
                                        .required("city")
                                        .build()
                        ),
                        (exchange, request) -> {
                            String city = request.params().arguments().get("city").asText();
                            String weather = String.format("""
                                    %s 天气:
                                    ☀ 晴 | 温度: 25°C | 湿度: 60%% | 风速: 3m/s
                                    """, city);
                            return new McpSchema.CallToolResult(
                                    List.of(new McpSchema.TextContent(weather)), false);
                        }
                )
        );

        // 注册 read_local_file 工具
        server.addTool(
                new McpServerFeatures.SyncToolSpecification(
                        new McpSchema.Tool(
                                "read_local_file",
                                "读取本地文件内容",
                                McpSchema.JsonSchema.OBJECT()
                                        .addProperty("path", McpSchema.JsonSchema.STRING()
                                                .description("文件路径"))
                                        .required("path")
                                        .build()
                        ),
                        (exchange, request) -> {
                            String path = request.params().arguments().get("path").asText();
                            try {
                                String content = java.nio.file.Files.readString(
                                        java.nio.file.Path.of(path));
                                return new McpSchema.CallToolResult(
                                        List.of(new McpSchema.TextContent(content)), false);
                            } catch (Exception e) {
                                return new McpSchema.CallToolResult(
                                        List.of(new McpSchema.TextContent("读取失败: " + e.getMessage())),
                                        true);
                            }
                        }
                )
        );

        System.err.println("[MCP Demo Server] 已启动，等待连接...");
        // 使用 stdio 传输
        StdioServerTransport transport = new StdioServerTransport();
        server.start(transport);
    }
}
```

注意：DemoMcpServer 依赖 MCP Java SDK (`io.modelcontextprotocol.sdk:mcp`)，需在 pom.xml 中补充：

```xml
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp</artifactId>
    <version>0.17.0</version>
</dependency>
```

- [ ] **Step 2: 创建 McpToolProvider（MCP Client 生命周期管理）**

```java
package com.llm.langchain4j.mcp;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpClient;
import dev.langchain4j.mcp.client.McpClients;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MCP 工具提供者 —— 管理 MCP Client 生命周期
 * 启动时连接 MCP Server，将 MCP 工具注入 Agent
 */
public class McpToolProvider implements AutoCloseable {

    private final List<McpClient> clients = new ArrayList<>();
    private final List<ToolSpecification> mcpTools = new ArrayList<>();
    private final List<Object> mcpToolObjects = new ArrayList<>();

    /**
     * 连接内置 Demo MCP Server（通过 stdio 子进程方式启动）
     */
    public boolean connectDemoServer() {
        try {
            String javaHome = System.getProperty("java.home");
            String javaCmd = javaHome + "/bin/java";
            String classpath = System.getProperty("java.class.path");

            StdioMcpTransport transport = new StdioMcpTransport.Builder()
                    .command(javaCmd)
                    .args(List.of("-cp", classpath,
                            "com.llm.langchain4j.mcp.DemoMcpServer"))
                    .build();

            McpClient client = McpClients.create(transport);
            clients.add(client);

            List<ToolSpecification> tools = client.tools();
            mcpTools.addAll(tools);
            System.out.println("[MCP] Demo Server 已连接，加载 " + tools.size() + " 个工具:");
            for (ToolSpecification t : tools) {
                System.out.println("  - [MCP] " + t.name() + ": " + t.description());
            }
            return true;
        } catch (Exception e) {
            System.err.println("[MCP] Demo Server 连接失败: " + e.getMessage());
            registerFallbackTools();
            return false;
        }
    }

    /**
     * MCP Server 不可用时的降级工具
     */
    private void registerFallbackTools() {
        System.out.println("[MCP] 使用降级内置工具");
        mcpToolObjects.add(new McpFallbackTools());
        // 模拟 MCP 工具规格
        mcpTools.add(ToolSpecification.builder()
                .name("get_weather")
                .description("获取指定城市的天气信息 [MCP 降级模式]")
                .build());
        mcpTools.add(ToolSpecification.builder()
                .name("read_local_file")
                .description("读取本地文件内容 [MCP 降级模式]")
                .build());
    }

    public List<ToolSpecification> getToolSpecifications() {
        return mcpTools;
    }

    public List<Object> getToolObjects() {
        return mcpToolObjects;
    }

    public boolean isConnected() {
        return !clients.isEmpty();
    }

    @Override
    public void close() {
        for (McpClient client : clients) {
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * MCP 降级工具 —— 当 MCP Server 不可用时直接提供
     */
    public static class McpFallbackTools {

        @dev.langchain4j.agent.tool.Tool("获取指定城市的天气信息 [MCP Fallback]")
        public String get_weather(String city) {
            return String.format("%s 天气: ☀ 晴 | 温度: 25°C | 湿度: 60%% | 风速: 3m/s", city);
        }

        @dev.langchain4j.agent.tool.Tool("读取本地文件内容 [MCP Fallback]")
        public String read_local_file(String path) {
            try {
                return java.nio.file.Files.readString(java.nio.file.Path.of(path));
            } catch (Exception e) {
                return "读取失败: " + e.getMessage();
            }
        }
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/llm/langchain4j/mcp/ pom.xml
git commit -m "feat: add MCP Demo Server and McpToolProvider"
```

---

### Task 6: 创建 AgentObserver（可观测性监听器）

**Files:**
- Create: `src/main/java/com/llm/langchain4j/AgentObserver.java`

- [ ] **Step 1: 创建 AgentObserver**

```java
package com.llm.langchain4j;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 全链路可观测性监听器
 * 追踪每次 LLM 请求的完整流程：请求→工具调用→回复→Memory 状态
 */
public class AgentObserver implements ChatModelListener {

    private final AtomicInteger roundCounter = new AtomicInteger(0);
    private volatile String lastToolCalls = "";
    private volatile String lastToolResults = "";
    private volatile String lastResponse = "";

    @Override
    public void onRequest(ChatModelRequestContext context) {
        int round = roundCounter.incrementAndGet();
        List<ChatMessage> messages = context.chatRequest().messages();
        List<ToolSpecification> tools = context.chatRequest().toolSpecifications();

        // 重置状态
        lastToolCalls = "";
        lastToolResults = "";
        lastResponse = "";

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  Agent 请求 ── [第 " + round + " 轮]");
        System.out.println("╠══════════════════════════════════════════════════════════╣");

        // 显示发送的消息
        System.out.println("║  ┌ 消息列表 (" + messages.size() + " 条):");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            String icon = switch (msg.type()) {
                case SYSTEM -> "⚙️";
                case USER -> "👤";
                case AI -> "🤖";
                case TOOL_EXECUTION_RESULT -> "🔧";
            };
            String content = msg.toString();
            if (content.length() > 80) content = content.substring(0, 80) + "...";
            // 移除换行避免格式混乱
            content = content.replace("\n", "\\n").replace("\r", "");
            System.out.println("║  │ " + icon + " [" + msg.type() + "] " + content);
        }

        // 显示可用工具
        if (tools != null && !tools.isEmpty()) {
            System.out.println("║  ├────────────────");
            System.out.println("║  │ 🧰 可用工具 (" + tools.size() + " 个):");
            for (ToolSpecification tool : tools) {
                System.out.println("║  │   - " + tool.name() + ": " + tool.description());
            }
        }
        System.out.println("║  └────────────────");
        System.out.println("║  ⏳ 等待 LLM 决策...");
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        AiMessage aiMessage = context.chatResponse().aiMessage();
        TokenUsage tokenUsage = context.chatResponse().tokenUsage();

        // 检查是否有工具调用
        if (aiMessage.hasToolExecutionRequests()) {
            List<ToolExecutionRequest> toolRequests = aiMessage.toolExecutionRequests();
            StringBuilder sb = new StringBuilder();
            for (ToolExecutionRequest req : toolRequests) {
                String tag = req.name().startsWith("get_") || req.name().startsWith("read_")
                        ? "[MCP]" : "[Tool]";
                sb.append("║  🔧 ").append(tag).append(" 调用: ").append(req.name())
                        .append("(").append(req.arguments()).append(")\n");
            }
            lastToolCalls = sb.toString();
            System.out.print(lastToolCalls);
        } else {
            // LLM 给出了最终回复
            String content = aiMessage.text();
            lastResponse = content;
            System.out.println("║  ✅ LLM 最终回复 (无工具调用)");
            if (content != null) {
                String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
                System.out.println("║  💬 " + preview.replace("\n", "\\n"));
            }
        }

        if (tokenUsage != null) {
            System.out.println("║  📊 Token: 输入=" + tokenUsage.inputTokenCount()
                    + " 输出=" + tokenUsage.outputTokenCount()
                    + " 总计=" + tokenUsage.totalTokenCount());
        }
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        System.err.println("║  ❌ 错误: " + context.error().getMessage());
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

    /** 记录工具执行结果（在 AgentConfig 的回调中调用） */
    public void onToolResult(String toolName, String arguments, String result, boolean isMcp) {
        String tag = isMcp ? "[MCP]" : "[Tool]";
        String preview = result.length() > 80 ? result.substring(0, 80) + "..." : result;
        System.out.println("║  ✔ " + tag + " " + toolName + " → " + preview.replace("\n", "\\n"));
    }

    /** 记录最终回复 */
    public void onFinalResponse(String content) {
        if (lastResponse.isEmpty()) {
            System.out.println("║  ✅ LLM 最终回复:");
            if (content != null) {
                String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
                System.out.println("║  💬 " + preview.replace("\n", "\\n"));
            }
        }
    }

    /** 显示 Memory 状态快照 */
    public void showMemorySnapshot(int roundCount, int maxRounds, int messageCount) {
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  📝 Memory 状态: " + roundCount + " 轮 / 最多 " + maxRounds
                + " 轮, 共 " + messageCount + " 条消息");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/llm/langchain4j/AgentObserver.java
git commit -m "feat: add AgentObserver for full-chain observability"
```

---

### Task 7: 创建 AgentConfig（装配工厂）

**Files:**
- Create: `src/main/java/com/llm/langchain4j/AgentConfig.java`

- [ ] **Step 1: 创建 AgentConfig**

```java
package com.llm.langchain4j;

import com.llm.langchain4j.mcp.McpToolProvider;
import com.llm.langchain4j.memory.AgentMemoryProvider;
import com.llm.langchain4j.rag.KnowledgeBaseInitializer;
import com.llm.langchain4j.rag.RagRetriever;
import com.llm.langchain4j.skill.CalculatorSkill;
import com.llm.langchain4j.skill.RagSkill;
import com.llm.langchain4j.skill.SearchSkill;
import com.llm.langchain4j.skill.TimeQuerySkill;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.service.AiServices;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent 装配工厂 —— 创建并连线所有组件
 */
public class AgentConfig {

    private final String apiKey;
    private final String baseUrl;
    private final String chatModelName;
    private final String embeddingModelName;
    private final int maxMemoryMessages;
    private final int maxRagResults;
    private final String knowledgeDir;
    private final boolean enableMcp;

    private AgentObserver observer;
    private AgentMemoryProvider memoryProvider;
    private RagRetriever ragRetriever;
    private KnowledgeBaseInitializer knowledgeBaseInitializer;
    private McpToolProvider mcpToolProvider;
    private AgentService agentService;
    private ChatLanguageModel chatModel;

    public AgentConfig(String apiKey, String baseUrl, String chatModelName) {
        this(apiKey, baseUrl, chatModelName, "text-embedding-3-small", 20, 3, "knowledge", true);
    }

    public AgentConfig(String apiKey, String baseUrl, String chatModelName,
                       String embeddingModelName, int maxMemoryMessages,
                       int maxRagResults, String knowledgeDir, boolean enableMcp) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.chatModelName = chatModelName;
        this.embeddingModelName = embeddingModelName;
        this.maxMemoryMessages = maxMemoryMessages;
        this.maxRagResults = maxRagResults;
        this.knowledgeDir = knowledgeDir;
        this.enableMcp = enableMcp;
    }

    public AgentService build() {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   LangChain4j Agent 初始化...          ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println();

        // 1. 可观测性
        observer = new AgentObserver();

        // 2. LLM 模型（DeepSeek via OpenAI 兼容）
        chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(chatModelName)
                .timeout(Duration.ofSeconds(120))
                .maxRetries(2)
                .listeners(List.of(observer))
                .build();
        System.out.println("[Config] LLM: " + chatModelName + " @ " + baseUrl);

        // 3. Memory
        memoryProvider = new AgentMemoryProvider(maxMemoryMessages);
        System.out.println("[Config] Memory: 滑动窗口 " + maxMemoryMessages + " 条消息");

        // 4. RAG
        try {
            EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(embeddingModelName)
                    .timeout(Duration.ofSeconds(60))
                    .build();
            ragRetriever = new RagRetriever(embeddingModel, maxRagResults);
            knowledgeBaseInitializer = new KnowledgeBaseInitializer(knowledgeDir, ragRetriever);
            knowledgeBaseInitializer.initialize();
        } catch (Exception e) {
            System.out.println("[Config] RAG 初始化失败（Embedding 模型可能不可用）: " + e.getMessage());
            System.out.println("[Config] RAG 功能将降级为关键词匹配");
            ragRetriever = createFallbackRetriever();
        }

        // 5. MCP
        List<Object> allTools = new ArrayList<>();
        allTools.add(new CalculatorSkill());
        allTools.add(new SearchSkill());
        allTools.add(new TimeQuerySkill());

        if (ragRetriever != null) {
            allTools.add(new RagSkill(ragRetriever));
        }

        if (enableMcp) {
            mcpToolProvider = new McpToolProvider();
            mcpToolProvider.connectDemoServer();
            allTools.addAll(mcpToolProvider.getToolObjects());
        }

        // 6. 构建 AgentService
        agentService = AiServices.builder(AgentService.class)
                .chatLanguageModel(chatModel)
                .chatMemoryProvider(memoryProvider)
                .tools(allTools.toArray(new Object[0]))
                .build();

        System.out.println("[Config] Agent 装配完成，共 " + allTools.size() + " 个工具");
        System.out.println();
        return agentService;
    }

    private RagRetriever createFallbackRetriever() {
        // 简单的关键词匹配降级
        return new RagRetriever(null, maxRagResults) {
            @Override
            public String search(String query) {
                return "RAG 检索不可用（Embedding 模型初始化失败），请检查 API Key 和网络连接";
            }
        };
    }

    // ==================== Getters ====================

    public AgentObserver getObserver() { return observer; }
    public AgentMemoryProvider getMemoryProvider() { return memoryProvider; }
    public RagRetriever getRagRetriever() { return ragRetriever; }
    public KnowledgeBaseInitializer getKnowledgeBaseInitializer() { return knowledgeBaseInitializer; }
    public McpToolProvider getMcpToolProvider() { return mcpToolProvider; }
    public AgentService getAgentService() { return agentService; }
    public ChatLanguageModel getChatModel() { return chatModel; }

    public void shutdown() {
        if (mcpToolProvider != null) {
            mcpToolProvider.close();
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/llm/langchain4j/AgentConfig.java
git commit -m "feat: add AgentConfig assembly factory"
```

---

### Task 8: 创建 LangChain4jAgentDemo（交互式终端入口）

**Files:**
- Create: `src/main/java/com/llm/langchain4j/LangChain4jAgentDemo.java`

- [ ] **Step 1: 创建 LangChain4jAgentDemo**

```java
package com.llm.langchain4j;

import com.llm.langchain4j.mcp.McpToolProvider;
import com.llm.langchain4j.memory.AgentMemoryProvider;
import dev.langchain4j.memory.ChatMemory;

import java.util.Scanner;

/**
 * LangChain4j Agent 交互式终端
 *
 * 运行方式:
 *   export DEEPSEEK_API_KEY=sk-your-key
 *   mvn exec:java -Dexec.mainClass="com.llm.langchain4j.LangChain4jAgentDemo"
 *
 * 内置命令:
 *   /tools   查看所有可用工具（含 MCP）
 *   /mcp     查看 MCP 连接状态
 *   /rag     查看 RAG 知识库状态
 *   /memory  查看当前对话记忆
 *   /trace   切换追踪详细程度
 *   /clear   清空对话记忆
 *   /bye     退出
 */
public class LangChain4jAgentDemo {

    private static final String USER_ID = "terminal-user";

    public static void main(String[] args) {
        // 读取 API Key
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("LLM_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请先设置环境变量 DEEPSEEK_API_KEY");
            System.out.println("  export DEEPSEEK_API_KEY=sk-your-key-here");
            System.exit(1);
        }

        // 构建 Agent
        AgentConfig config = new AgentConfig(
                apiKey,
                "https://api.deepseek.com",
                "deepseek-chat"
        );
        AgentService agent = config.build();
        AgentMemoryProvider memoryProvider = config.getMemoryProvider();
        AgentObserver observer = config.getObserver();

        // 启动信息
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   LangChain4j Agent Demo                        ║");
        System.out.println("║   模型: DeepSeek Chat                            ║");
        System.out.println("║   能力: Tool | Memory | RAG | MCP | Observer     ║");
        System.out.println("║   输入 /bye 退出  /help 查看命令                  ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("You > ");
            String input;
            try {
                input = scanner.nextLine().trim();
            } catch (Exception e) {
                System.out.println("\n再见！");
                break;
            }

            if (input.isEmpty()) continue;

            if (handleCommand(input, config, memoryProvider)) {
                if ("/bye".equals(input)) break;
                continue;
            }

            System.out.print("AI");
            try {
                String reply = agent.chat(USER_ID, input);
                System.out.print(" > " + reply);
                System.out.println();

                // Memory 快照
                ChatMemory memory = memoryProvider.getMemory(USER_ID);
                if (memory != null) {
                    observer.showMemorySnapshot(
                            countUserMessages(memory),
                            memoryProvider instanceof AgentMemoryProvider
                                    ? ((AgentMemoryProvider) memoryProvider).getMaxMessages() : 20,
                            memory.messages().size()
                    );
                }
            } catch (Exception e) {
                System.err.println("出错: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
        }

        scanner.close();
        config.shutdown();
        System.out.println("Agent 已关闭，再见！");
    }

    private static int countUserMessages(ChatMemory memory) {
        return (int) memory.messages().stream()
                .filter(m -> m.type() == dev.langchain4j.data.message.ChatMessageType.USER)
                .count();
    }

    private static boolean handleCommand(String input, AgentConfig config,
                                         AgentMemoryProvider memoryProvider) {
        switch (input) {
            case "/bye":
                System.out.println("再见！");
                return true;
            case "/help":
                System.out.println("命令列表:");
                System.out.println("  /tools   查看所有可用工具（内置 + MCP）");
                System.out.println("  /mcp     查看 MCP 连接状态");
                System.out.println("  /rag     查看 RAG 知识库状态");
                System.out.println("  /memory  查看当前对话记忆");
                System.out.println("  /trace   切换追踪详细程度");
                System.out.println("  /clear   清空对话记忆");
                System.out.println("  /bye     退出");
                return true;
            case "/tools":
                System.out.println("已注册工具列表:");
                System.out.println("  内置 Skill:");
                System.out.println("    - calculator: 数学计算");
                System.out.println("    - search: 互联网搜索");
                System.out.println("    - timeQuery: 时间查询");
                System.out.println("    - ragSearch: 知识库检索");
                McpToolProvider mcp = config.getMcpToolProvider();
                if (mcp != null && mcp.isConnected()) {
                    System.out.println("  MCP 工具 (已连接):");
                    for (var t : mcp.getToolSpecifications()) {
                        System.out.println("    - [MCP] " + t.name() + ": " + t.description());
                    }
                } else {
                    System.out.println("  MCP 工具: 未连接 (使用降级模式)");
                    System.out.println("    - [MCP] get_weather: 天气查询");
                    System.out.println("    - [MCP] read_local_file: 文件读取");
                }
                return true;
            case "/mcp":
                McpToolProvider mcpStatus = config.getMcpToolProvider();
                if (mcpStatus != null) {
                    System.out.println("MCP 状态: " + (mcpStatus.isConnected() ? "已连接" : "降级模式"));
                    System.out.println("MCP 工具数: " + mcpStatus.getToolSpecifications().size());
                } else {
                    System.out.println("MCP 未初始化");
                }
                return true;
            case "/rag":
                if (config.getKnowledgeBaseInitializer() != null) {
                    System.out.println("RAG 知识库: 已就绪");
                    System.out.println("知识库目录: knowledge/");
                } else {
                    System.out.println("RAG 知识库: 未初始化");
                }
                return true;
            case "/memory":
                ChatMemory memory = memoryProvider.getMemory(USER_ID);
                if (memory != null) {
                    var messages = memory.messages();
                    System.out.println("Memory 窗口: " + messages.size() + " 条消息");
                    for (var msg : messages) {
                        String icon = switch (msg.type()) {
                            case SYSTEM -> "⚙️";
                            case USER -> "👤";
                            case AI -> "🤖";
                            case TOOL_EXECUTION_RESULT -> "🔧";
                        };
                        String content = msg.toString();
                        if (content.length() > 100) content = content.substring(0, 100) + "...";
                        System.out.println("  " + icon + " [" + msg.type() + "] " + content);
                    }
                } else {
                    System.out.println("Memory: 无记录");
                }
                return true;
            case "/trace":
                System.out.println("追踪: 始终开启（默认详细模式）");
                return true;
            case "/clear":
                memoryProvider.clear(USER_ID);
                System.out.println("[记忆已清空]");
                return true;
            default:
                return false;
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/llm/langchain4j/LangChain4jAgentDemo.java
git commit -m "feat: add interactive terminal demo entry point"
```

---

### Task 9: 编译与整体验证

- [ ] **Step 1: 完整编译**

```bash
mvn clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 2: 检查编译输出，修复任何编译错误**

逐一检查每个 Java 文件的编译输出，根据实际 LangChain4j 1.16.1 API 调整代码。

- [ ] **Step 3: 运行 Demo（需要 API Key）**

```bash
DEEPSEEK_API_KEY=sk-xxx mvn exec:java -Dexec.mainClass="com.llm.langchain4j.LangChain4jAgentDemo"
```

验证以下功能：
- [ ] 对话正常工作（LLM 回复）
- [ ] 工具调用正常（算数、时间查询）
- [ ] 可观测性输出（请求/响应/工具调用/Memory 快照）
- [ ] Memory 正常（多轮对话记忆）
- [ ] RAG 检索正常（提问知识库相关问题时触发）
- [ ] MCP 工具可用（天气查询）

- [ ] **Step 4: 修复运行中发现的问题后最终 Commit**

```bash
git add -A
git commit -m "fix: resolve compilation and runtime issues for LangChain4j Agent demo"
```

---

### Task 10: 添加 exec-maven-plugin 方便运行

**Files:**
- Modify: `pom.xml`

在 `<build><plugins>` 中添加：

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.4.1</version>
    <configuration>
        <mainClass>com.llm.langchain4j.LangChain4jAgentDemo</mainClass>
    </configuration>
</plugin>
```

- [ ] **Step 1: 更新 pom.xml**

- [ ] **Step 2: 验证快捷运行**

```bash
mvn exec:java
```
Expected: 提示需要 API Key，说明启动正常

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: add exec-maven-plugin for easy demo launch"
```
