package com.llm.springai;

import com.llm.springai.mcp.McpIntegration;
import com.llm.springai.memory.AgentMemoryManager;
import com.llm.springai.rag.RagRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlibabaAgentService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final RagRetriever ragRetriever;
    private final McpIntegration mcpIntegration;
    private final Map<String, AgentObserver> observers = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
            你是一个AI 助手，具备以下能力：
            - 使用计算器进行数学计算
            - 使用搜索引擎获取最新信息
            - 查询当前时间和日期
            - 调用 MCP 工具获取天气和文件信息

            回答简洁、准确。当你需要更多信息时，主动使用工具获取。
            """;

    public AlibabaAgentService(ChatClient chatClient,
                                AgentMemoryManager memoryManager,
                                RagRetriever ragRetriever,
                                McpIntegration mcpIntegration) {
        this.chatClient = chatClient;
        this.chatMemory = memoryManager.getChatMemory();
        this.ragRetriever = ragRetriever;
        this.mcpIntegration = mcpIntegration;
    }

    public String chat(String userId, String message) {
        AgentObserver observer = observers.computeIfAbsent(userId, id -> new AgentObserver());
        observer.onRequest(userId, message);

        try {
            String reply = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                    .call()
                    .content();

            observer.onResponse(reply);
            return reply;
        } catch (Exception e) {
            observer.onError(e.getMessage());
            return "抱歉，处理您的请求时出错了: " + e.getMessage();
        }
    }

    public void clear(String userId) {
        chatMemory.clear(userId);
        observers.remove(userId);
    }

    public RagRetriever getRagRetriever() { return ragRetriever; }
    public McpIntegration getMcpIntegration() { return mcpIntegration; }
    public AgentObserver getObserver(String userId) { return observers.get(userId); }
    public ChatClient getChatClient() { return chatClient; }
}
