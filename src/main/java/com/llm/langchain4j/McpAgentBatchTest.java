package com.llm.langchain4j;

import com.llm.langchain4j.mcp.McpToolProvider;

/**
 * Batch test: Agent + MCP real connection, verify the LLM can call MCP weather tools.
 *
 * Run: mvn compile -q && CP=$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout):target/classes && java -cp "$CP" com.llm.langchain4j.McpAgentBatchTest
 */
public class McpAgentBatchTest {

    private static final String USER_ID = "test-user";

    public static void main(String[] args) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("LLM_API_KEY");
        }

        System.out.println("=== LangChain4j Agent + MCP 集成测试 ===\n");

        AgentConfig config = new AgentConfig(apiKey, "https://api.deepseek.com", "deepseek-chat");
        AgentService agent = config.build();
        McpToolProvider mcp = config.getMcpToolProvider();

        // Verify MCP connection
        System.out.println("\n--- MCP 状态 ---");
        System.out.println("MCP 连接: " + (mcp != null && mcp.isConnected()));
        System.out.println("MCP 模式: " + (mcp != null && mcp.isRealMcp() ? "真实 MCP" : "降级"));
        if (mcp != null) {
            System.out.println("MCP 工具: " + mcp.getToolSpecifications().size() + " 个");
        }

        System.out.println("\n--- 对话测试 ---\n");

        // Test: Ask weather - LLM should call MCP get_weather tool via ToolProvider
        String question = "北京现在天气怎么样？";
        System.out.println("Q: " + question);
        try {
            String reply = agent.chat(USER_ID, question);
            System.out.println("A: " + reply);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n--- 测试完成 ---");
        config.shutdown();
    }
}
