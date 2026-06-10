package com.llm.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对话记忆 —— 滑动窗口保留最近 N 轮对话
 *
 * 一轮 = 一条 user 消息 + 一条 assistant 回复（不含 system 消息和 tool 消息）
 * 窗口超过 maxRounds 时自动丢弃最早的轮次
 */
public class ChatMemory {

    private final int maxRounds;
    private final List<LlmClient.Message> messages = new ArrayList<>();

    /** 系统提示词（始终保留，不参与轮次计数） */
    private String systemPrompt;

    public ChatMemory(int maxRounds) {
        if (maxRounds < 1) throw new IllegalArgumentException("maxRounds 至少为 1");
        this.maxRounds = maxRounds;
    }

    /** 设置系统提示词（始终保留在上下文最前面，不计入轮次） */
    public void setSystemPrompt(String prompt) {
        this.systemPrompt = prompt;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    /** 添加一条消息 */
    public void add(LlmClient.Message message) {
        messages.add(message);
        evictIfNeeded();
    }

    /** 便捷方法：添加用户消息 */
    public void addUser(String content) {
        add(new LlmClient.Message("user", content));
    }

    /** 便捷方法：添加助手消息 */
    public void addAssistant(String content) {
        add(new LlmClient.Message("assistant", content));
    }

    /** 获取当前窗口内的全部消息（含 system prompt，供发送给 LLM） */
    public List<LlmClient.Message> getContextMessages() {
        List<LlmClient.Message> result = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            result.add(new LlmClient.Message("system", systemPrompt));
        }
        result.addAll(messages);
        return result;
    }

    /** 仅获取用户和助手的对话消息（不含 system prompt 和 tool 消息） */
    public List<LlmClient.Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /** 当前窗口内的轮次数 */
    public int currentRounds() {
        int rounds = 0;
        for (LlmClient.Message m : messages) {
            if ("user".equals(m.role)) rounds++;
        }
        return rounds;
    }

    /** 窗口大小上限 */
    public int getMaxRounds() {
        return maxRounds;
    }

    /** 清空对话历史（保留 system prompt） */
    public void clear() {
        messages.clear();
    }

    // ---- 滑动窗口淘汰 ----

    private void evictIfNeeded() {
        // 以 user 角色计数轮次，超出 maxRounds 时淘汰最早的整轮
        while (currentRounds() > maxRounds) {
            evictOneRound();
        }
    }

    private void evictOneRound() {
        // 找到第一条 user 消息并淘汰它及之前的所有消息，直到下一条 user 消息之前
        int firstUserIdx = -1;
        for (int i = 0; i < messages.size(); i++) {
            if ("user".equals(messages.get(i).role)) {
                firstUserIdx = i;
                break;
            }
        }
        if (firstUserIdx == -1) return;

        // 淘汰 [0, firstUserIdx] 范围的消息（含 tool 消息）
        // 然后继续删除紧跟的 assistant 和 tool 消息，直到遇到下一条 user 或列表结束
        int removeEnd = firstUserIdx;
        // 找这条 user 后面的 assistant + tool 消息
        for (int i = firstUserIdx + 1; i < messages.size(); i++) {
            String role = messages.get(i).role;
            if ("assistant".equals(role) || "tool".equals(role)) {
                removeEnd = i;
            } else {
                break;
            }
        }

        // 从后往前删避免索引偏移
        for (int i = removeEnd; i >= 0; i--) {
            messages.remove(i);
        }
    }
}