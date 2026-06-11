package com.llm.springai.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Component;

@Component
public class AgentMemoryManager {

    private final ChatMemory chatMemory;
    private final int maxMessages;

    public AgentMemoryManager() {
        this(20);
    }

    public AgentMemoryManager(int maxMessages) {
        this.maxMessages = maxMessages;
        this.chatMemory = MessageWindowChatMemory.builder().build();
    }

    public ChatMemory getChatMemory() {
        return chatMemory;
    }

    public void clear(String userId) {
        chatMemory.clear(userId);
    }

    public int getMaxMessages() {
        return maxMessages;
    }
}
