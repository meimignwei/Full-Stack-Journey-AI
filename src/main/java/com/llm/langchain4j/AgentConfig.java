package com.llm.langchain4j;

import com.llm.langchain4j.mcp.McpToolProvider;
import com.llm.langchain4j.memory.AgentMemoryProvider;
import com.llm.langchain4j.rag.KnowledgeBaseInitializer;
import com.llm.langchain4j.rag.RagRetriever;
import com.llm.langchain4j.skill.CalculatorSkill;
import com.llm.langchain4j.skill.RagSkill;
import com.llm.langchain4j.skill.SearchSkill;
import com.llm.langchain4j.skill.TimeQuerySkill;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.service.AiServices;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

        // 1. Observer
        observer = new AgentObserver();

        // 2. LLM Model
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
            int docCount = knowledgeBaseInitializer.initialize();
            System.out.println("[Config] RAG: 已初始化, 加载 " + docCount + " 个文档");
        } catch (Exception e) {
            System.out.println("[Config] RAG 初始化失败: " + e.getMessage());
            System.out.println("[Config] RAG 将使用降级模式");
            ragRetriever = null;
        }

        // 5. Tools
        List<Object> allTools = new ArrayList<>();
        allTools.add(new CalculatorSkill());
        allTools.add(new SearchSkill());
        allTools.add(new TimeQuerySkill());

        if (ragRetriever != null) {
            allTools.add(new RagSkill(ragRetriever));
        }

        // 6. MCP
        if (enableMcp) {
            mcpToolProvider = new McpToolProvider();
            mcpToolProvider.connectDemoServer();
            allTools.addAll(mcpToolProvider.getToolObjects());
        }

        // 7. Build Agent
        agentService = AiServices.builder(AgentService.class)
                .chatLanguageModel(chatModel)
                .chatMemoryProvider(memoryProvider)
                .tools(allTools.toArray(new Object[0]))
                .build();

        System.out.println("[Config] Agent 装配完成, 共 " + allTools.size() + " 个工具");
        System.out.println();
        return agentService;
    }

    // Getters
    public AgentObserver getObserver() { return observer; }
    public AgentMemoryProvider getMemoryProvider() { return memoryProvider; }
    public RagRetriever getRagRetriever() { return ragRetriever; }
    public KnowledgeBaseInitializer getKnowledgeBaseInitializer() { return knowledgeBaseInitializer; }
    public McpToolProvider getMcpToolProvider() { return mcpToolProvider; }
    public AgentService getAgentService() { return agentService; }
    public ChatLanguageModel getChatModel() { return chatModel; }

    public void shutdown() {
        if (mcpToolProvider != null) mcpToolProvider.close();
    }
}