package com.llm.springai;

import com.llm.springai.mcp.McpIntegration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Batch test: Agent + MCP, verify the LLM can call tools.
 *
 * Run:
 *   export DASHSCOPE_API_KEY=sk-your-alibaba-key
 *   mvn compile exec:java -Dexec.mainClass="com.llm.springai.AlibabaAgentBatchTest"
 */
@SpringBootApplication
public class AlibabaAgentBatchTest implements CommandLineRunner {

    private static final String USER_ID = "test-user";

    private final AlibabaAgentService agentService;
    private final McpIntegration mcp;

    public AlibabaAgentBatchTest(AlibabaAgentService agentService, McpIntegration mcp) {
        this.agentService = agentService;
        this.mcp = mcp;
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请先设置环境变量 DASHSCOPE_API_KEY");
            System.exit(1);
        }
        SpringApplication.run(AlibabaAgentBatchTest.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("=== Spring AI Alibaba Agent + MCP 集成测试 ===\n");

        System.out.println("\n--- MCP 状态 ---");
        System.out.println("MCP 连接: " + (mcp != null && mcp.isConnected()));
        System.out.println("MCP 模式: " + (mcp != null && mcp.isRealMcp() ? "真实 MCP" : "降级"));

        System.out.println("\n--- 对话测试 ---\n");

        String question = "深圳现在天气怎么样？";
        System.out.println("Q: " + question);
        try {
            String reply = agentService.chat(USER_ID, question);
            System.out.println("A: " + reply);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n--- 测试完成 ---");
    }
}
