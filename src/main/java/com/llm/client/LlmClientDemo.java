package com.llm.client;

/**
 * 使用示例 —— 展示 LlmClient 的全部功能：
 * 基础对话、工具注册（搜索/计算器/时间查询）、对话记忆（滑动窗口）
 */
public class LlmClientDemo {

    public static void main(String[] args) {
        // ---- 1. 初始化客户端（默认 DeepSeek） ----
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("LLM_API_KEY");
        }
        LlmClient client = new LlmClient(
                apiKey,
                "https://api.deepseek.com",
                "deepseek-chat"
        );

        // ---- 2. 注册工具 ----
        ToolRegistry tools = new ToolRegistry()
                .register(new SearchTool())
                .register(new CalculatorTool())
                .register(new TimeQueryTool());

        // ---- 3. 初始化对话记忆（保留最近 5 轮） ----
        ChatMemory memory = new ChatMemory(5);
        memory.setSystemPrompt("你是一个有用的助手，可以使用工具来帮助用户。回答要简洁。");

        // ---- 4. 装配到客户端 ----
        client.withTools(tools).withMemory(memory);

        // ==================== 场景演示 ====================

        // 场景 A：工具调用 —— 计算器
        System.out.println("=== 用户: 帮我算一下 (38 * 47) + sqrt(256) ===");
        String replyA = client.sendMessage("帮我算一下 (38 * 47) + sqrt(256)");
        System.out.println("助手: " + replyA);
        System.out.println("当前记忆轮次: " + memory.currentRounds());

        // 场景 B：工具调用 —— 时间查询
        System.out.println("\n=== 用户: 今天是星期几？几号？ ===");
        String replyB = client.sendMessage("今天是星期几？几号？");
        System.out.println("助手: " + replyB);

        // 场景 C：工具调用 —— 搜索
        System.out.println("\n=== 用户: 帮我搜索一下最近 AI 领域的新闻 ===");
        String replyC = client.sendMessage("帮我搜索一下最近 AI 领域的新闻");
        System.out.println("助手: " + replyC);

        // 场景 D：多轮对话记忆 —— 验证 LLM 记得之前的对话
        System.out.println("\n=== 用户: 我刚才问的第一个问题是什么？ ===");
        String replyD = client.sendMessage("我刚才问的第一个问题是什么？");
        System.out.println("助手: " + replyD);
        System.out.println("当前记忆轮次: " + memory.currentRounds());

        // ==================== 手动控制（不使用记忆） ====================

        System.out.println("\n=== 无记忆直接调用（不计入历史） ===");
        String directReply = client.chat("你好，1+1 等于几？");
        System.out.println("助手: " + directReply);
        System.out.println("记忆轮次（未变）: " + memory.currentRounds());

        // ---- 查看滑动窗口状态 ----
        System.out.println("\n=== 记忆窗口状态 ===");
        System.out.println("窗口上限: " + memory.getMaxRounds() + " 轮");
        System.out.println("当前轮次: " + memory.currentRounds());
        System.out.println("全部上下文消息数: " + memory.getContextMessages().size());
        for (LlmClient.Message m : memory.getMessages()) {
            String preview = m.content != null && m.content.length() > 50
                    ? m.content.substring(0, 50) + "..."
                    : m.content;
            System.out.printf("  [%s] %s | toolCallId=%s | toolCalls=%s%n",
                    m.role, preview,
                    m.toolCallId, m.toolCalls);
        }
    }
}