package com.llm.langchain4j;

import com.llm.langchain4j.mcp.McpToolProvider;
import com.llm.langchain4j.memory.AgentMemoryProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.data.message.ChatMessageType;

import java.util.Scanner;

/**
 * LangChain4j Agent 交互式终端
 *
 * 运行方式:
 *   export DEEPSEEK_API_KEY=sk-your-key
 *   mvn compile exec:java -Dexec.mainClass="com.llm.langchain4j.LangChain4jAgentDemo"
 *
 * 内置命令:
 *   /tools  /mcp  /rag  /memory  /trace  /clear  /bye
 */
public class LangChain4jAgentDemo {

    private static final String USER_ID = "terminal-user";

    public static void main(String[] args) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("LLM_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请先设置环境变量 DEEPSEEK_API_KEY");
            System.out.println("  export DEEPSEEK_API_KEY=sk-your-key-here");
            System.exit(1);
        }

        AgentConfig config = new AgentConfig(apiKey, "https://api.deepseek.com", "deepseek-chat");
        AgentService agent = config.build();
        AgentMemoryProvider memoryProvider = config.getMemoryProvider();
        AgentObserver observer = config.getObserver();

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

            try {
                String reply = agent.chat(USER_ID, input);
                System.out.println("AI > " + reply);

                ChatMemory memory = memoryProvider.getMemory(USER_ID);
                if (memory != null) {
                    int userCount = (int) memory.messages().stream()
                            .filter(m -> m.type() == ChatMessageType.USER).count();
                    observer.showMemorySnapshot(userCount,
                            memoryProvider.getMaxMessages(),
                            memory.messages().size());
                }
            } catch (Exception e) {
                System.err.println("出错: " + e.getMessage());
            }
            System.out.println();
        }

        scanner.close();
        config.shutdown();
        System.out.println("Agent 已关闭，再见！");
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
                System.out.println("  /clear   清空对话记忆");
                System.out.println("  /bye     退出");
                return true;
            case "/tools":
                System.out.println("已注册工具:");
                System.out.println("  内置 Skill: calculator, search, timeQuery, ragSearch");
                McpToolProvider mcp = config.getMcpToolProvider();
                if (mcp != null && mcp.isConnected()) {
                    System.out.println("  MCP 工具 (" + mcp.getToolSpecifications().size() + " 个):");
                    for (var t : mcp.getToolSpecifications()) {
                        System.out.println("    - [MCP] " + t.name() + ": " + t.description());
                    }
                }
                return true;
            case "/mcp":
                McpToolProvider ms = config.getMcpToolProvider();
                if (ms != null) {
                    System.out.println("MCP 状态: " + (ms.isConnected() ? "已连接(降级模式)" : "未连接"));
                    System.out.println("MCP 工具数: " + ms.getToolSpecifications().size());
                } else {
                    System.out.println("MCP 未启用");
                }
                return true;
            case "/rag":
                if (config.getKnowledgeBaseInitializer() != null) {
                    System.out.println("RAG 知识库: 已就绪 (knowledge/)");
                } else {
                    System.out.println("RAG 知识库: 未初始化");
                }
                return true;
            case "/memory":
                ChatMemory mem = memoryProvider.getMemory(USER_ID);
                if (mem != null) {
                    var msgs = mem.messages();
                    System.out.println("Memory: " + msgs.size() + " 条消息");
                    for (var msg : msgs) {
                        String icon = switch (msg.type()) {
                            case SYSTEM -> "⚙️";
                            case USER -> "👤";
                            case AI -> "🤖";
                            case TOOL_EXECUTION_RESULT -> "🔧";
                            case CUSTOM -> "📎";
                        };
                        String content = msg.toString();
                        if (content.length() > 100) content = content.substring(0, 100) + "...";
                        System.out.println("  " + icon + " [" + msg.type() + "] " + content);
                    }
                } else {
                    System.out.println("Memory: 无记录");
                }
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