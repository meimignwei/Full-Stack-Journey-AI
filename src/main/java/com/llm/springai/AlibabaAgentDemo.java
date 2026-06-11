package com.llm.springai;

import com.llm.springai.mcp.McpIntegration;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

/**
 * Spring AI Alibaba Agent 交互式终端
 *
 * 运行方式:
 *   export DASHSCOPE_API_KEY=sk-your-alibaba-key
 *   mvn compile exec:java -Dexec.mainClass="com.llm.springai.AlibabaAgentDemo"
 *
 * 内置命令:
 *   /tools  /mcp  /rag  /memory  /clear  /bye
 */
@SpringBootApplication
public class AlibabaAgentDemo implements CommandLineRunner {

    private static final String USER_ID = "terminal-user";

    private final AlibabaAgentService agentService;
    private final ChatModel chatModel;

    public AlibabaAgentDemo(AlibabaAgentService agentService, ChatModel chatModel) {
        this.agentService = agentService;
        this.chatModel = chatModel;
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请先设置环境变量 DASHSCOPE_API_KEY");
            System.out.println("  export DASHSCOPE_API_KEY=sk-your-alibaba-key");
            System.exit(1);
        }
        SpringApplication.run(AlibabaAgentDemo.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   Spring AI Alibaba Agent Demo                  ║");
        System.out.println("║   模型: DashScope (通义千问)                      ║");
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

            if (handleCommand(input)) {
                if ("/bye".equals(input)) break;
                continue;
            }

            try {
                String reply = agentService.chat(USER_ID, input);
                System.out.println("AI > " + reply);
            } catch (Exception e) {
                System.err.println("出错: " + e.getMessage());
            }
            System.out.println();
        }

        scanner.close();
        System.out.println("Agent 已关闭，再见！");
    }

    private boolean handleCommand(String input) {
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
                System.out.println("  /clear   清空对话记忆");
                System.out.println("  /bye     退出");
                return true;
            case "/tools":
                System.out.println("已注册工具:");
                System.out.println("  内置 Skill: calculator, search, timeQuery");
                System.out.println("  MCP 降级工具: get_weather, read_local_file");
                System.out.println("  模型: " + chatModel.getClass().getSimpleName());
                return true;
            case "/mcp":
                McpIntegration mcp = agentService.getMcpIntegration();
                if (mcp != null) {
                    String mode = mcp.isRealMcp() ? "真实 MCP" : "降级模式";
                    System.out.println("MCP 状态: " + (mcp.isConnected() ? "已连接 - " + mode : "未连接"));
                } else {
                    System.out.println("MCP 未启用");
                }
                return true;
            case "/rag":
                if (agentService.getRagRetriever() != null) {
                    System.out.println("RAG 知识库: 已就绪 (knowledge/)");
                } else {
                    System.out.println("RAG 知识库: 未初始化");
                }
                return true;
            case "/memory":
                System.out.println("Memory: 用户 " + USER_ID + " — 由 MessageChatMemoryAdvisor 管理");
                return true;
            case "/clear":
                agentService.clear(USER_ID);
                System.out.println("[记忆已清空]");
                return true;
            default:
                return false;
        }
    }
}
