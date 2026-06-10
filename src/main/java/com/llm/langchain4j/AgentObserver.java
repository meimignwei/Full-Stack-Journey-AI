package com.llm.langchain4j;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AgentObserver implements ChatModelListener {

    private final AtomicInteger roundCounter = new AtomicInteger(0);

    @Override
    public void onRequest(ChatModelRequestContext context) {
        int round = roundCounter.incrementAndGet();
        List<ChatMessage> messages = context.chatRequest().messages();
        List<dev.langchain4j.agent.tool.ToolSpecification> tools = context.chatRequest().toolSpecifications();

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  Agent 请求 ── [第 " + round + " 轮]");
        System.out.println("╠══════════════════════════════════════════════════════════╣");

        System.out.println("║  ┌ 消息列表 (" + messages.size() + " 条):");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            String icon = switch (msg.type()) {
                case SYSTEM -> "⚙️";
                case USER -> "👤";
                case AI -> "🤖";
                case TOOL_EXECUTION_RESULT -> "🔧";
                case CUSTOM -> "📎";
            };
            String content = msg.toString();
            if (content.length() > 100) content = content.substring(0, 100) + "...";
            content = content.replace("\n", "\\n").replace("\r", "");
            System.out.println("║  │ " + icon + " [" + msg.type() + "] " + content);
        }

        if (tools != null && !tools.isEmpty()) {
            System.out.println("║  ├────────────────");
            System.out.println("║  │ 🧰 可用工具 (" + tools.size() + " 个):");
            for (var tool : tools) {
                String desc = tool.description() != null && tool.description().length() > 60
                        ? tool.description().substring(0, 60) + "..."
                        : tool.description();
                System.out.println("║  │   - " + tool.name() + ": " + desc);
            }
        }
        System.out.println("║  └────────────────");
        System.out.println("║  ⏳ 等待 LLM 决策...");
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        AiMessage aiMessage = context.chatResponse().aiMessage();
        TokenUsage tokenUsage = context.chatResponse().tokenUsage();

        if (aiMessage.hasToolExecutionRequests()) {
            List<ToolExecutionRequest> toolRequests = aiMessage.toolExecutionRequests();
            for (ToolExecutionRequest req : toolRequests) {
                String tag = req.name().startsWith("get_") || req.name().startsWith("read_")
                        ? "[MCP]" : "[Tool]";
                String args = req.arguments() != null && req.arguments().length() > 80
                        ? req.arguments().substring(0, 80) + "..."
                        : req.arguments();
                System.out.println("║  🔧 " + tag + " 调用: " + req.name() + "(" + args + ")");
            }
        } else {
            String content = aiMessage.text();
            System.out.println("║  ✅ LLM 最终回复 (无工具调用)");
            if (content != null) {
                String preview = content.length() > 120 ? content.substring(0, 120) + "..." : content;
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

    public void onToolResult(String toolName, String arguments, String result, boolean isMcp) {
        String tag = isMcp ? "[MCP]" : "[Tool]";
        String preview = result != null && result.length() > 80 ? result.substring(0, 80) + "..." : result;
        System.out.println("║  ✔ " + tag + " " + toolName + " → " + (preview != null ? preview.replace("\n", "\\n") : ""));
    }

    public void onFinalResponse(String content) {
        System.out.println("║  ✅ LLM 最终回复:");
        if (content != null) {
            String preview = content.length() > 120 ? content.substring(0, 120) + "..." : content;
            System.out.println("║  💬 " + preview.replace("\n", "\\n"));
        }
    }

    public void showMemorySnapshot(int roundCount, int maxRounds, int messageCount) {
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  📝 Memory 状态: " + roundCount + " 轮 / 最多 " + maxRounds
                + " 轮, 共 " + messageCount + " 条消息");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}