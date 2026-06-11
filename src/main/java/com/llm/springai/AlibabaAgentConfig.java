package com.llm.springai;

import com.llm.springai.mcp.McpIntegration;
import com.llm.springai.memory.AgentMemoryManager;
import com.llm.springai.rag.KnowledgeBaseInitializer;
import com.llm.springai.rag.RagRetriever;
import com.llm.springai.tool.CalculatorTool;
import com.llm.springai.tool.SearchTool;
import com.llm.springai.tool.TimeQueryTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlibabaAgentConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel,
                                  AgentMemoryManager memoryManager,
                                  CalculatorTool calculatorTool,
                                  SearchTool searchTool,
                                  TimeQueryTool timeQueryTool,
                                  McpIntegration.WeatherTool weatherTool,
                                  McpIntegration.FileReaderTool fileReaderTool) {
        return ChatClient.builder(chatModel)
                .defaultTools(
                        calculatorTool,
                        searchTool,
                        timeQueryTool,
                        weatherTool,
                        fileReaderTool
                )
                .defaultAdvisors(
                        new TraceableAdvisor(),
                        MessageChatMemoryAdvisor.builder(memoryManager.getChatMemory()).build()
                )
                .build();
    }

    // ── RAG ──

    @Bean
    @ConditionalOnMissingBean
    public RagRetriever ragRetriever(EmbeddingModel embeddingModel) {
        return new RagRetriever(embeddingModel, 3);
    }

    @Bean
    @ConditionalOnMissingBean
    public KnowledgeBaseInitializer knowledgeBaseInitializer(RagRetriever ragRetriever,
                                                              EmbeddingModel embeddingModel) {
        return new KnowledgeBaseInitializer("knowledge", ragRetriever, embeddingModel);
    }

    // ── MCP (fallback tools) ──

    @Bean
    public McpIntegration.WeatherTool weatherTool() {
        return new McpIntegration.WeatherTool();
    }

    @Bean
    public McpIntegration.FileReaderTool fileReaderTool() {
        return new McpIntegration.FileReaderTool();
    }
}