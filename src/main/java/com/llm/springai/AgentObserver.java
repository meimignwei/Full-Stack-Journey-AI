package com.llm.springai;

import java.util.concurrent.atomic.AtomicInteger;

public class AgentObserver {

    private final AtomicInteger roundCounter = new AtomicInteger(0);

    public void onRequest(String userId, String message) {
        int round = roundCounter.incrementAndGet();
        System.out.println();
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  Agent 请求 ── [第 " + round + " 轮]");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  👤 [" + userId + "] " + truncate(message, 60));
        System.out.println("║  ⏳ 等待 LLM 决策...");
    }

    public void onResponse(String content) {
        System.out.println("║  ✅ LLM 回复:");
        System.out.println("║  💬 " + truncate(content, 120));
        System.out.println("╚══════════════════════════════════════════╝");
    }

    public void onError(String error) {
        System.err.println("║  ❌ 错误: " + error);
        System.out.println("╚══════════════════════════════════════════╝");
    }

    public void onToolCall(String toolName, String result) {
        System.out.println("║  🔧 工具: " + toolName + " → " + truncate(result, 80));
    }

    public void showMemory(int userIdMessageCount, int max, int totalMessages) {
        System.out.println("  📝 Memory: " + userIdMessageCount + " 条用户消息 / 最多 " + max + " 条, 共 " + totalMessages + " 条记录");
    }

    private static String truncate(String s, int max) {
        if (s == null) return "(null)";
        String cleaned = s.replace("\n", "\\n").replace("\r", "");
        return cleaned.length() > max ? cleaned.substring(0, max) + "…" : cleaned;
    }
}
