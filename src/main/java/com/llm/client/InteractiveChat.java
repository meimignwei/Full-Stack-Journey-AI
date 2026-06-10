package com.llm.client;

import java.util.Scanner;

/**
 * 交互式终端对话入口 —— 默认使用 DeepSeek
 *
 * 运行方式:
 *   export DEEPSEEK_API_KEY=sk-your-key
 *   java com.llm.client.InteractiveChat
 *
 * 内置命令:
 *   /tools  查看已注册的工具
 *   /memory 查看对话记忆窗口
 *   /clear  清空对话记忆
 *   /bye    退出
 */
public class InteractiveChat {

    public static void main(String[] args) {
        // ---- 读取 API Key ----
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("LLM_API_KEY"); // 兼容旧变量名
        }
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请先设置环境变量 DEEPSEEK_API_KEY");
            System.out.println("  export DEEPSEEK_API_KEY=sk-your-key-here");
            System.exit(1);
        }

        // ---- DeepSeek 客户端 ----
        LlmClient client = new LlmClient(
                apiKey,
                "https://api.deepseek.com",
                "deepseek-chat"
        );

        // ---- 注册工具 ----
        ToolRegistry tools = new ToolRegistry()
                .register(new SearchTool())
                .register(new CalculatorTool())
                .register(new TimeQueryTool());

        // ---- 对话记忆（最近 10 轮） ----
        ChatMemory memory = new ChatMemory(10);
        memory.setSystemPrompt("你是一个有用的助手。你可以使用计算器、搜索和时间查询工具来帮助用户。回答简洁、准确。");

        // ---- 工具调用监听器（实时显示调用过程） ----
        LlmClient.ToolCallListener toolListener = (toolName, arguments, result) -> {
            System.out.println("  [工具调用] " + toolName + "(" + arguments + ")");
            // 结果太长则截断
            String shortResult = result.length() > 100 ? result.substring(0, 100) + "..." : result;
            System.out.println("  [工具结果] " + shortResult);
        };

        client.withTools(tools)
              .withMemory(memory)
              .withToolCallListener(toolListener)
              .withDebug(true); // 默认开启调试，显示完整请求/响应 JSON

        // ---- 启动 ----
        System.out.println();
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║    DeepSeek 终端对话                 ║");
        System.out.println("║    工具: 搜索 | 计算器 | 时间查询    ║");
        System.out.println("║    记忆: 最近 10 轮  调试: 开启      ║");
        System.out.println("║    输入 /bye 退出  /help 查看命令    ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("You > ");
            String input;
            try {
                input = scanner.nextLine().trim();
            } catch (Exception e) {
                // Ctrl+D / EOF
                System.out.println("\n再见！");
                break;
            }

            if (input.isEmpty()) continue;

            // ---- 内置命令 ----
            if (handleCommand(input, memory, tools, client)) {
                if ("/bye".equals(input)) break;
                continue;
            }

            // ---- 发送消息 ----
            try {
                String reply = client.sendMessage(input);
                System.out.println("AI > " + reply);
            } catch (Exception e) {
                System.err.println("出错: " + e.getMessage());
            }
            System.out.println();
        }
        scanner.close();
    }

    private static boolean handleCommand(String input, ChatMemory memory, ToolRegistry tools, LlmClient client) {
        switch (input) {
            case "/bye":
                System.out.println("再见！");
                return true;
            case "/help":
                System.out.println("命令列表:");
                System.out.println("  /tools   查看已注册的工具");
                System.out.println("  /memory  查看对话记忆窗口");
                System.out.println("  /debug   切换调试模式（打印完整请求/响应 JSON）");
                System.out.println("  /clear   清空对话记忆");
                System.out.println("  /bye     退出");
                return true;
            case "/debug":
                boolean newDebug = !client.isDebug();
                client.withDebug(newDebug);
                System.out.println("[调试模式: " + (newDebug ? "开启" : "关闭") + "]");
                return true;
            case "/tools":
                System.out.println("已注册工具 (" + tools.size() + " 个):");
                for (String name : tools.getToolNames()) {
                    Tool t = tools.get(name);
                    System.out.println("  " + name + " — " + t.getDescription());
                }
                return true;
            case "/memory":
                System.out.println("窗口: " + memory.currentRounds() + " / " + memory.getMaxRounds() + " 轮");
                if (memory.getSystemPrompt() != null) {
                    System.out.println("  [system] " + memory.getSystemPrompt());
                }
                for (LlmClient.Message m : memory.getMessages()) {
                    String tag = m.role;
                    if (m.toolCalls != null && !m.toolCalls.isEmpty()) {
                        tag = tag + " → 调用: " + m.toolCalls.get(0).functionName;
                    }
                    if (m.toolCallId != null) {
                        tag = tag + " (tool)";
                    }
                    String preview = m.content != null && m.content.length() > 80
                            ? m.content.substring(0, 80) + "..."
                            : m.content;
                    System.out.println("  [" + tag + "] " + preview);
                }
                return true;
            case "/clear":
                memory.clear();
                System.out.println("[记忆已清空]");
                return true;
            default:
                return false;
        }
    }
}