package com.llm.springai;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Full-chain observable advisor — hooks into every LLM call within the agent loop.
 *
 * Each function-calling round goes through adviseCall(), so we see:
 *   1. Messages sent to LLM (user, tool results, etc.)
 *   2. Tool execution requests from LLM
 *   3. Final text response
 *   4. Token usage per round
 *
 * Output mirrors the langchain4j AgentObserver visual style.
 */
public class TraceableAdvisor implements CallAdvisor {

    private final AtomicInteger roundCounter = new AtomicInteger(0);

    @Override
    public String getName() {
        return "traceable-advisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        int round = roundCounter.incrementAndGet();
        Prompt prompt = request.prompt();
        List<Message> messages = prompt.getInstructions();

        // ── BEFORE: request tracing ──
        printHeader(round);
        printMessages(messages);
        printWaiting();

        // ── Execute ──
        ChatClientResponse response = chain.nextCall(request);

        // ── AFTER: response tracing ──
        ChatResponse chatResponse = response.chatResponse();
        Generation generation = chatResponse.getResult();
        AssistantMessage aiMessage = generation.getOutput();

        if (aiMessage.hasToolCalls()) {
            printToolCalls(aiMessage);
        } else {
            printFinalResponse(aiMessage);
        }

        printTokenUsage(chatResponse);

        // Reset counter when LLM gives final answer (no more tool calls)
        if (!aiMessage.hasToolCalls()) {
            roundCounter.set(0);
        }

        return response;
    }

    // ── Output helpers ──

    private void printHeader(int round) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  Agent 请求 ── [第 " + round + " 轮]");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
    }

    private void printMessages(List<Message> messages) {
        System.out.println("║  ┌ 消息列表 (" + messages.size() + " 条):");
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            String icon = messageIcon(msg);
            String role = messageRole(msg);
            String content = formatMessageContent(msg);
            System.out.println("║  │ " + icon + " [" + role + "] " + truncate(content, 90));
        }
        System.out.println("║  └────────────────");
    }

    private void printWaiting() {
        System.out.println("║  ⏳ 等待 LLM 决策...");
    }

    private void printToolCalls(AssistantMessage aiMessage) {
        List<AssistantMessage.ToolCall> toolCalls = aiMessage.getToolCalls();
        for (AssistantMessage.ToolCall tc : toolCalls) {
            String args = tc.arguments();
            if (args != null && args.length() > 80) {
                args = args.substring(0, 80) + "…";
            }
            System.out.println("║  🔧 调用工具: " + tc.name() + "(" + args + ")");
        }
    }

    private void printFinalResponse(AssistantMessage aiMessage) {
        System.out.println("║  ✅ LLM 最终回复 (无工具调用)");
        String text = aiMessage.getText();
        if (text != null) {
            System.out.println("║  💬 " + truncate(text, 120));
        }
    }

    private void printTokenUsage(ChatResponse chatResponse) {
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage != null) {
            System.out.println("║  📊 Token: 输入=" + usage.getPromptTokens()
                    + " 输出=" + usage.getCompletionTokens()
                    + " 总计=" + usage.getTotalTokens());
        }
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

    private String messageIcon(Message msg) {
        if (msg instanceof org.springframework.ai.chat.messages.SystemMessage) return "⚙️";
        if (msg instanceof org.springframework.ai.chat.messages.UserMessage)   return "👤";
        if (msg instanceof AssistantMessage am && am.hasToolCalls())           return "🤖→🔧";
        if (msg instanceof AssistantMessage)                                   return "🤖";
        if (msg instanceof org.springframework.ai.chat.messages.ToolResponseMessage) return "🔧";
        return "  ";
    }

    private String messageRole(Message msg) {
        if (msg instanceof AssistantMessage am && am.hasToolCalls()) return "assistant(tool_calls)";
        return msg.getMessageType().toString();
    }

    private String formatMessageContent(Message msg) {
        if (msg instanceof AssistantMessage am && am.hasToolCalls()) {
            List<AssistantMessage.ToolCall> tcs = am.getToolCalls();
            List<String> names = tcs.stream().map(AssistantMessage.ToolCall::name).toList();
            return "→ 调用 " + String.join(", ", names);
        }
        String text = msg.getText();
        return text != null ? text : "(无文本)";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "(null)";
        String cleaned = s.replace("\n", "\\n").replace("\r", "");
        return cleaned.length() > max ? cleaned.substring(0, max) + "…" : cleaned;
    }
}